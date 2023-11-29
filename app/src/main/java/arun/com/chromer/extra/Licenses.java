package arun.com.chromer.extra;

import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense30;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

/**
 * Created by Arun on 20/12/2015.
 */
public class Licenses {
    public static Notices getNotices() {
        Notices notices = new Notices();

        notices.addNotice(new Notice(
                "Lynket",
                "https://github.com/arunkumar9t2/lynket-browser",
                "Copyright (C) 2017 Arunkumar",
                new GnuGeneralPublicLicense30()
        ));

        notices.addNotice(new Notice(
                "LicensesDialog",
                "http://psdev.de",
                "Copyright 2013 Philip Schiffer <admin@psdev.de>",
                new ApacheSoftwareLicense20()
        ));

        notices.addNotice(new Notice(
                "Material Dialogs",
                "https://github.com/afollestad/material-dialogs",
                "Copyright (c) 2015 Aidan Michael Follestad",
                new MITLicense()
        ));

        notices.addNotice(new Notice(
                "Android Open Source Project",
                "https://source.android.com/",
                "Copyright (C) 2008 The Android Open Source Project",
                new ApacheSoftwareLicense20()
        ));

        notices.addNotice(new Notice(
                "Timber",
                "https://github.com/JakeWharton/timber",
                "Copyright 2013 Jake Wharton",
                new ApacheSoftwareLicense20()
        ));

        return notices;
    }
}
