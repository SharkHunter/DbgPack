# Synopsis <a name="Synopsis"></a>

This is a plugin for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) (PMS) which makes it easy to gather
debug information.

# Installation <a name="Install"></a>

* download the [jar file](https://github.com/downloads/SharkHunter/Channel/tv_plug_058.jar) and place it in the PMS `plugins` directory
* restart PMS

## Uninstalling <a name="Uninstall"></a>

To uninstall the debug packer, remove the jar file from the `plugins` directory and restart PMS. 

## Usage ##
Under the General configuration tab there is a button called "Pack Debug Info" press this when needed to gather debug info.
A dialog box appears which allows you to select if certain files should be included in the debug pack. Once all is done press the
pack debug button and in the `PMS` directory a file called `pms_dbg.zip` has been created. This file contains all logs needed for PMS
and PMS plugin developers to find out the problem.

## Developers ##
The DbgPacker packs the PMS.conf,WEB.conf and the PMS log file (debug.log) plus any file that plugins says should be included. To include new
files into teh debug packer add the following code snippet:

```
String f=(String)PMS.getConfiguration().getCustomProperty("dbgpack");
if(f==null)
	f=myFile;
else 
	f=myFile+","+f;
PMS.getConfiguration().setCustomProperty("dbgpack",f);
PMS.getConfiguration().save();
```

This adds myFile to the list of files that will be packed. Do this when the plugin starts up since the information is not read until the dump is ordered. 
If the file is missing when the pack is to be performed it will be ignored.
