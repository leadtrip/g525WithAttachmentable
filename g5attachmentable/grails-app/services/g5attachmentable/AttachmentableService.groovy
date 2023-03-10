/* Copyright 2010 Mihai Cazacu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package g5attachmentable


import g5attachmentable.core.exceptions.AttachmentableException
import g5attachmentable.core.exceptions.EmptyFileException
import g5attachmentable.util.AttachmentableUtil
import grails.gorm.transactions.Transactional
import grails.orm.PagedResultList
import org.apache.commons.io.FilenameUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.lang.reflect.UndeclaredThrowableException

@Transactional
class AttachmentableService {

    def grailsApplication

    /* -------------------------- ATTACHMENT LINK --------------------------- */

    AttachmentLink getAttachmentLink(Long attachmentId) {
        def link = AttachmentLink.withCriteria(uniqueResult: true) {
            attachments {
                idEq attachmentId
            }
        }
        link
    }

    /* ------------------------------- POSTER ------------------------------- */

    def getPoster(Attachment attachment) {
      attachment.poster
    }

    /* ----------------------------- ATTACHMENT ----------------------------- */

    // add

    /**
     * Upload a list of files.
     * @param poster
     * @param reference
     * @param files
     * @return a list of successfully uploaded files
     */
    List<MultipartFile> upload(def poster,
                               def reference,
                               List<MultipartFile> files) {
        def uploadedFiles = []

        try {
            Attachment.withTransaction { status ->
                files.each {MultipartFile file ->
                    try {
                        addAttachment(poster, reference, file)
                        uploadedFiles << file
                    } catch (Exception e) {
                        if (e instanceof EmptyFileException) {
                            log.error "Error adding attachment: ${e.message}"
                        } else if (e instanceof UndeclaredThrowableException
                                && e.cause instanceof EmptyFileException) {
                            log.error "Error adding attachment: ${e.cause.message}"
                        } else {
                            status.setRollbackOnly()
                            log.error "Error adding attachment", e
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error "Error adding attachment: ${e.message}"
        }

        uploadedFiles
    }

    def addAttachment(def poster, def reference, CommonsMultipartFile file) {
        doAddAttachment(grailsApplication.config, poster, reference, file)
    }

    def addAttachment(def poster, def reference, MultipartFile file) {
        doAddAttachment(grailsApplication.config, poster, reference, file)
    }

    private Object doAddAttachment(def config,
                      def poster,
                      def reference,
                      def file) {

        if (reference.ident() == null) {
            throw new AttachmentableException(
                "You must save the entity [${delegate}] before calling addAttachment.")
        }

        if (!file?.size) {
            throw new EmptyFileException(file.name, file.originalFilename)
        }

        String delegateClassName = AttachmentableUtil.fixClassName(reference.class)
        String posterClass = (poster instanceof String) ? poster : AttachmentableUtil.fixClassName(poster.class.name)
        Long posterId = (poster instanceof String) ? 0L : poster.id
        String filename = file.originalFilename

        // link
        def link = AttachmentLink.findByReferenceClassAndReferenceId(
                delegateClassName, reference.ident())
        if (!link) {
            link = new AttachmentLink(
                    referenceClass: delegateClassName,
                    referenceId: reference.ident())
        }

        // attachment
        Attachment attachment = new Attachment(
                // file
                name: FilenameUtils.getBaseName(filename),
                ext: FilenameUtils.getExtension(filename),
                length: 0L,
                contentType: file.contentType,
                // poster
                posterClass: posterClass,
                posterId: posterId,
                // input
                inputName: file.name)
        link.addToAttachments attachment

        if (!link.save(flush: true)) {
            throw new AttachmentableException(
                    "Cannot create Attachment for arguments [$poster, $file], they are invalid.")
        }

        // save file to disk
        File diskFile = AttachmentableUtil.getFile(config, attachment, true)
        file.transferTo(diskFile)

        attachment.length = diskFile.length()

        // interceptors
        if(reference.respondsTo('onAddAttachment')) {
            reference.onAddAttachment(attachment)
        }

        attachment.save(flush:true) // Force update so searchable can try to index it again.

        return reference
    }

    // remove

    int removeAttachments(def reference) {
        def cnt = 0
        def dir = AttachmentableUtil.getDir(grailsApplication.config, reference)
        def files = []
        reference.getAttachments()?.collect {
            files << AttachmentableUtil.getFile(grailsApplication.config, it)
        }

        def lnk = AttachmentLink.findByReferenceClassAndReferenceId(
                reference.class.name, reference.ident())
        if (lnk) {
            try {
                lnk.delete(flush: true)
                files.each {File file ->
                    cnt++
                    AttachmentableUtil.delete(file)
                }
            } catch (DataIntegrityViolationException e) {
                log.error "Error deleting attachments: ${e.message}"
            }

            if (cnt) {
                AttachmentableUtil.delete(dir)
            }
        }

        cnt
    }

    boolean removeAttachment(Long attachmentId) {
        removeAttachment(Attachment.get(attachmentId))
    }

    boolean removeAttachment(Attachment attachment) {
        File file = AttachmentableUtil.getFile(grailsApplication.config, attachment)
        try {
            AttachmentLink lnk = attachment.lnk
            lnk.removeFromAttachments(attachment)
            attachment.delete(flush: true)
            AttachmentableUtil.delete(file)
            removeUnusedLinks()
            return true
        } catch (DataIntegrityViolationException e) {
            log.error "Error deleting attachment: ${e.message}"
        }

        false
    }

    int removeAttachments(def reference, List inputNames) {
        def cnt = 0
        def attachments = AttachmentLink.executeQuery("""
            select a
                Attachment a inner join a.lnk link
            where
                a.inputName in (:inputNames)
                and
                link.referenceClass = :referenceClass
                and
                link.referenceId = :referenceId""",
            [referenceClass: reference.class.name, referenceId: reference.ident(),
                    inputNames: inputNames])

        attachments?.each { Attachment attachment ->
            File file = AttachmentableUtil.getFile(grailsApplication.config, attachment)

            try {
                attachment.delete(flush: true)
                cnt++
                AttachmentableUtil.delete(file)
            } catch (DataIntegrityViolationException e) {
                log.error "Error deleting attachments: ${e.message}"
            }
        }

        removeUnusedLinks()

        cnt
    }

    private int removeUnusedLinks() {
        int result = Attachment.executeUpdate(
                'delete from AttachmentLink link where link.attachments is empty')
        result
    }

    // count

    int countAttachmentsByReference(def reference, List inputNames = []) {
        if (!reference) {
            throw new AttachmentableException(
                    "Reference is null.")
        }

        if (!reference.ident()) {
            throw new AttachmentableException(
                    "Reference [$reference] is not a persisted instance.")
        }

        int result = Attachment.createCriteria().get {
            projections {
                rowCount()
            }
            if (inputNames) {
                inList 'inputName', inputNames
            }
            lnk {
                eq 'referenceClass', reference.class.name
                eq 'referenceId', reference.ident()
            }
            cache true
        }
        result
    }

    int countAttachmentsByPoster(def poster) {
        if (!poster) {
            throw new AttachmentableException("Poster is null.")
        }

        if (! (poster instanceof String) && !poster.id) {
            throw new AttachmentableException(
                    "Poster [$poster] is not a persisted instance.")
        }

        int result = Attachment.createCriteria().get {
            projections {
                rowCount()
            }
            eq "posterClass", (poster instanceof String) ? poster : poster.class.name
            eq 'posterId', (poster instanceof String) ? 0L : poster.id
            cache true
        }

        result
    }

    // find

    PagedResultList findAttachmentsByPoster(def poster, def params = [:]) {
        if (!poster) {
            throw new AttachmentableException("Poster is null.")
        }

        if (! (poster instanceof String) && !poster.id) {
            throw new AttachmentableException(
                    "Poster [$poster] is not a persisted instance.")
        }

        params.order = params.order ?: 'desc'
        params.sort = params.sort ?: 'dateCreated'
        params.cache = true

        PagedResultList result = Attachment.createCriteria().list(params) {
            eq "posterClass", (poster instanceof String) ? poster : poster.class.name
            eq 'posterId', (poster instanceof String) ? 0L : poster.id
        }

        result
    }

    PagedResultList findAttachmentsByReference(def reference, List inputs = [], Map params = [:]) {
        if (!reference) {
            throw new AttachmentableException(
                    "Reference is null.")
        }

        if (!reference.ident()) {
            throw new AttachmentableException(
                    "Reference [$reference] is not a persisted instance.")
        }

        params.order = params.order ?: 'desc'
        params.sort = params.sort ?: 'dateCreated'
        params.cache = true

        PagedResultList result = Attachment.createCriteria().list(params) {
            if (inputs) {
                inList 'inputName', inputs
            }
            lnk {
                eq 'referenceClass', reference.class.name
                eq 'referenceId', reference.ident()
            }
        }

        result
    }

}
