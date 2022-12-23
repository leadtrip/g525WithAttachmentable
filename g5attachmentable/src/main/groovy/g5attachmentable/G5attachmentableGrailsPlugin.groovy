package g5attachmentable

import g5attachmentable.core.Attachmentable
import g5attachmentable.core.ajax.AjaxMultipartResolver
import g5attachmentable.core.ajax.ProgressDescriptor
import g5attachmentable.core.exceptions.AttachmentableException
import g5attachmentable.util.AttachmentableUtil
import grails.config.Config
import grails.plugins.*
import grails.util.GrailsClassUtils
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest

class G5attachmentableGrailsPlugin extends Plugin {

    static LOG = LoggerFactory.getLogger('g5attachmentable.G5attachmentableGrailsPlugin')

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "5.2.5 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "G5attachmentable" // Headline display name of the plugin
    def author = "Mike W"
    def authorEmail = ""
    def description = '''\
Grails 5 version of attachmentable plugin.
A plugin that allows you to add attachments to domain classes in a generic manner
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/g5attachmentable"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->
        multipartResolver(AjaxMultipartResolver) {
            def cfg = config.grails.attachmentable

            maxInMemorySize = cfg?.maxInMemorySize ? cfg.maxInMemorySize as int : 1024
            maxUploadSize = cfg?.maxUploadSize ? cfg.maxUploadSize as long : 1024000000

            if (cfg?.uploadTempDir) {
                uploadTempDir = cfg.uploadTempDir
            }
        }

        // poster evaluator
        def evaluator = config.grails.attachmentable?.poster?.evaluator
        if (!evaluator) {
            evaluator = { request.user }
            config.grails.attachmentable.poster.evaluator = evaluator
            LOG.debug "Attachmentable config(poster evaluator): 'request.user'"
        }

        attachmentFileConverter(FileContentConverter) {}

        }
    }

    void doWithDynamicMethods() {
        AttachmentableService service = applicationContext.getBean('attachmentableService')
        def config = config

        // upload dir
        fixUploadDir grailsApplication

        // enhance controllers
        grailsApplication.controllerClasses?.each {c ->
            addControllerMethods config, c.clazz.metaClass, service
        }

        // enhance domain classes
        grailsApplication.domainClasses?.each {d ->
            if (Attachmentable.class.isAssignableFrom(d.clazz) || getAttachmentableProperty(d)) {
                addDomainMethods d.clazz.metaClass, service
            }
        }
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    /**
     * Implement code that is executed when any artefact that this plugin is
     * watching is modified and reloaded. The event contains: event.source,
     * event.application, event.manager, event.ctx, and event.plugin.
     * @param event
     */
    void onChange(Map<String, Object> event) {
        def ctx = event.ctx

        if (event.source && ctx && event.application) {
            AttachmentableService service = ctx.getBean('attachmentableService')
            def config = grailsApplication.config

            // upload dir
            if ('Config'.equals(event.source.name)) {
                fixUploadDir grailsApplication
            }
            // enhance domain class
            else if (grailsApplication.isDomainClass(event.source)) {
                def c = grailsApplication.getDomainClass(event.source.name)
                if (Attachmentable.class.isAssignableFrom(c) || getAttachmentableProperty(c)) {
                    addDomainMethods c.metaClass, service
                }
            }
            // enhance controller
            else if (grailsApplication.isControllerClass(event.source)) {
                def c = grailsApplication.getControllerClass(event.source.name)
                addControllerMethods config, c.metaClass, service
            }
        }
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

    public static final String ATTACHMENTABLE_PROPERTY_NAME = "attachmentable";

    private static getAttachmentableProperty(domainClass) {
        GrailsClassUtils.getStaticPropertyValue(domainClass.clazz, ATTACHMENTABLE_PROPERTY_NAME)
    }

    private void addDomainMethods( MetaClass mc,
                                   AttachmentableService service) {
        // add

        mc.addAttachment = { def poster,
                             CommonsMultipartFile file ->
            service.addAttachment(poster, delegate, file)
        }

        // get

        mc.getAttachments = {String inputName, params = [:] ->
            getAttachments(inputName ? [inputName] : [], params)
        }

        mc.getAttachments = {List inputNames = [], params = [:] ->
            service.findAttachmentsByReference(delegate, inputNames, params)
        }

        // count

        mc.getTotalAttachments = {String inputName ->
            getTotalAttachments(inputName ? [inputName] : [])
        }

        mc.getTotalAttachments = {List inputNames = [] ->
            service.countAttachmentsByReference(delegate, inputNames)
        }

        // remove

        mc.removeAttachments = {->
            service.removeAttachments(delegate)
        }

        mc.removeAttachments = {List inputNames ->
            service.removeAttachments(delegate, inputNames)
        }

        mc.removeAttachment = {Attachment attachment ->
            service.removeAttachment(attachment)
        }

        mc.removeAttachment = {Long attachmentId ->
            service.removeAttachment(attachmentId)
        }

        mc.removeAttachment = {String inputName ->
            removeAttachments([inputName])
        }

    }

    private void addControllerMethods( Config config,
                                      MetaClass mc,
                                      AttachmentableService service) {
        mc.uploadStatus = {
            def controllerInstance = delegate
            def request = controllerInstance.request

            ProgressDescriptor pd = request.session[AjaxMultipartResolver.progressAttrName(request)]
            controllerInstance.render(pd ?: '')
        }

        mc.attachUploadedFilesTo = {reference, List inputNames = [] ->
            def controllerInstance = delegate
            def request = controllerInstance.request

            if (AttachmentableUtil.isAttachmentable(reference)) {
                // user
                def evaluator = config.grails.attachmentable.poster.evaluator
                def user = null

                if (evaluator instanceof Closure) {
                    evaluator.delegate = controllerInstance
                    evaluator.resolveStrategy = Closure.DELEGATE_ONLY
                    user = evaluator.call()
                }

                if (!user) {
                    throw new AttachmentableException(
                            "No [grails.attachmentable.poster.evaluator] setting defined or the evaluator doesn't evaluate to an entity or string. Please define the evaluator correctly in grails-app/conf/Config.groovy or ensure attachmenting is secured via your security rules.")
                }

                if (!(user instanceof String) && !user.id) {
                    throw new AttachmentableException(
                            "The evaluated Attachment poster is not a persistent instance.")
                }

                // files
                List<MultipartFile> filesToUpload = []
                List<MultipartFile> uploadedFiles = []

                if (request instanceof DefaultMultipartHttpServletRequest || request instanceof StandardMultipartHttpServletRequest) {
                    request.multipartFiles.each {k, v ->
                        if (!inputNames || inputNames.contains(k)) {
                            if (v instanceof List) {
                                v.each {MultipartFile file ->
                                    filesToUpload << file
                                }
                            } else {
                                filesToUpload << v
                            }
                        }
                    }

                    // upload
                    uploadedFiles = service.upload(user, reference, filesToUpload)
                }

                // result
                [filesToUpload: filesToUpload, uploadedFiles: uploadedFiles]
            }
        }

        mc.existAttachments = {
            def controllerInstance = delegate
            def request = controllerInstance.request
            def result = false

            if (request instanceof DefaultMultipartHttpServletRequest) {
                request.multipartFiles.each {k, v ->
                    if (v instanceof List) {
                        v.each { MultipartFile file ->
                            if (file.size)
                                result = true
                        }
                    } else {
                        if (v.size)
                            result = true
                    }
                }
            }

            return result
        }
    }

    private void fixUploadDir(application) {
        def dir = application.config.grails.attachmentable?.uploadDir
        if (!dir) {
            String userHome  = System.properties.'user.home'
            String appName   = application.metadata.getApplicationName()
            dir = new File(userHome, appName).canonicalPath
            application.config.grails.attachmentable.uploadDir = dir
        }
        LOG.debug "Attachmentable config(upload dir): '$dir'"
    }

}
