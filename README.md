A simple grails 5 app with an inlined grails 5 version of the attachmentable plugin.

It includes a Person domain to which you can attach files.

As is, the files will be uploaded to your `~/coreGrailsApp/coregrailsapp.Person `directory.

This branch uses the original jquery versions found with the plugin which aren't compatible with the version of bootstrap bundled with grails 5.

Versions are:
* jquery - 1.4.2
* jquery-ui - 1.8rc3
* jquery-MultiFile - v1.46
* jquery-form - 2.43

If you just swap out the version of jquery for the version bundled with grails 5 (3.5.1) this doesn't work, you need to update the other jquery plugins, see the latest_jquery branch. 