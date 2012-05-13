# Synopsis <a name="Synopsis"></a>

This is a plugin for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) (PMS) which makes it easy to gather
and view debug information.

# Installation <a name="Install"></a>

* download the [jar file](https://github.com/downloads/SharkHunter/DbgPack/dbgpack_017.jar) and place it in the PMS `plugins` directory

* restart PMS

## Uninstalling <a name="Uninstall"></a>

To uninstall the debug packer, remove the jar file from the `plugins` directory and restart PMS.

## Usage ##
Under the General Configuration tab there is a button called "View and Zip Logs". Press this when needed to gather the files. A dialog box appears which allows you to select if certain files should be included in the debug pack. Once all is done press the "Zip selected files" button and in the `PMS` directory a file called `pms_dbg.zip` has been created. This file contains all logs needed for PMS and PMS plugin developers to find out the problem.

You can also view or create any of the registered files by pressing the buttons on the right. A greyed-out item means the file doesn't exist yet.

## Adding Files ##
Files can be added manually to DbgPack by setting `dbgpack` to a comma-separated list of files in PMS.conf:

	dbgpack = c:\\path\\to\\mylog,c:\\path\\to\\myconf

## Developers ##
The DbgPacker packs the PMS.conf, WEB.conf and the PMS log file (debug.log) plus any files that plugins say should be included. To include new files into the debug packer add 'dbgpack.java' to your project and implement the `dbgpack` interface in your main plugin class:

	public Object dbgpack_cb() {
		return mylog;
	}

or if you have several files:

	public Object dbgpack_cb() {
		return new String[] {mylog, myconf};
	}

This adds `mylog` and/or `myconf` to the list of files that will be packed. If the file is missing when the pack is to be performed it will be ignored.


