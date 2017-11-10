Authors:
John Thomas aka Chilastra.Oarun
Simon Gronlund aka Chimaera.Zimoon

In order to get up and running with the development of SWGAide, you're going to 
need a few things. A java JDK, a java IDE, integrated or stand-alone source 
control, and the SWGAide code base.
This document will walk you through setting up your Java JDK, Eclipse (a java 
IDE), Subclipse (integrated subversion source control for Eclipse), connecting 
to SWGAide at SourceForge, and getting the SWGAide project started in Eclipse.
Other equally good SVN-utilities for Eclipse exist and you can use whichever.

## Download and Install your Java JDK: ##
Download a recent JDK 8 from http://java.sun.com/javase/downloads/index.jsp
and install. This will give you all the java development stuff you'll need to
develop, compile, and run java applications on your machine.

## Download and Install Eclipse: ##
Download the Eclipse IDE for Java from http://www.eclipse.org/downloads/ and
download Eclipse. There is an 83M and 163M version, either should work just fine
though. Once downloaded, unzip the file to whatever location you decide to keep
it, I went with C:\Program Files\Eclipse. For convenience, create a shortcut to
eclipse.exe on your desktop. Double click to launch.

## Install Subclipse into Eclipse: ##
The dialogs and options may be slightly different between versions, but for
all intents and purposes they should be relatively similar. At the Help menu
click on "Install New Software..." and click "Add..." to add a new site. Type
http://subclipse.tigris.org/update_1.6.x in the popup and click "OK". This will
show you all the available add-ons available at this site. At the very least,
you will need to select JavaHL Adapter, Subclipse, and SVNKit Adapter. Click
"Install". Click "Next" on the install dialog. Accept the license agreement
and click "Finish". You will need to restart Eclipse after this, but it might
be better to restart the machine.

Installing other SVN-utilities works the same way. 

## Connect to SWGAide at SourceForge and Create Your SWGAide Project: ##
Launch Eclipse, click on File->Import. 
Open the SVN branch, select "Checkout Projects from SVN", and click "Next". 
Make sure "Create a new repository location" is selected and click "Next". 
Type https://swgaide.svn.sourceforge.net/svnroot/swgaide/trunk into the URL
edit box and click "Next". Once it is connected, select the "SWG" folder and
click "Next". Make sure the selected option is "Check out as project configured
using the New Project Wizard" and click "Finish".

At the next dialog, select your wizard type: open "Java", select "Java Project",
and click "Next". Enter a name for the project, for example SWGAide, then select
the option "Use project folder as root for sources and class files", and click
"Finish". This starts pulling all the SWGAide source code.

## Generate Resource Class Files ##
The more than 800 resource-class files are generated locally. Now and after any
update download http://sites.google.com/site/simongronlund/resourcetree2.xml
Navigate to swg/tools/SWGResourceClassGenerator.java, open it, navigate to its
main method, and read the usage comment. In Eclipse, open "Run" -> "Run As...",
and open the "Run Configurations" wizard for the generator. At "Arguments", add
a TO path and a FROM path to the "Program Arguments". The TO-path is the path to
your project's location, not including "swg". The FROM-path is the path for the
XML file. Example:
"C:\Documents and Settings\Simon\Projects\SWGAide_0_8_00"
"C:\Documents and Settings\Simon\Projects\resourcetree2\resourcetree2.xml"
At the wizard, select "Apply" and "Run".

Now the generator creates swg/crafting/resources/SWGResourceInfo.java and the
more than 800 java files in the swg/crafting/resources/types/ package. Usually
Eclipse triggers a rebuild, otherwise you build manually.

## Sort Out Possible Problems ##
Usually you should be fine by now, however, if there is anything you need to
sort out I ask you to take notes. If it is a general issue, update this file and
commit it to the repository. If it is a local issue, post a note at the SWGCraft
forum first so we make an initial assessment.

## Developer Notes ##
In the code I have now begun using the key word DEVELOPER_NOTE, search for it to
find notes at locations of interest. They are not many, but if there is anything
a developer must now, e.g. some boolean flag used while developing, please use
this key word.

## Launch SWGAide ##
Once this far it is time to launch SWGAide.
Remember that SWGAide moves in-game mails to its current working directory.
Personally I use the very same folder for development as for my general SWGAide
installation, that is the project folder. Yes, a bit risky but on the other hand
I never have to worry about mails scattered over several locations. As long as
no incompatibility issue is added to the SWGAide.DAT file during development
everything should be fine.

Navigate to and select swg/SWGAide.java, right-click and select "Run As..." ->
"Java Application". If you want to you can open "Run Configuration" and at the
"Arguments" add "-ea" to the "VM Arguments", this enables "assert" wherever it
is used during development. Launch and run SWGAide as you are used to.

## Create SWGAide.jar ##
To create SWGAide.jar you just edit the file stub-SWGAide.jardesc according to
the comment early in that file. Should I (Zimoon) disappear for a longer period,
feel free to edit SWGAide, host it and its required version file at a new site,
and continue on this project.

## SWGAide at SWGCraft ##
SWGAide begun as a project at SWGCraft to help traders. Today most of the more
powerful features are about supporting SWGCraft. Sobuno has kindly created a
private forum board for recognized SWGAide developers; send Zimoon a PM with
your introduction and your SWGCraft handle.
Links:
Board » Development Forums » Third Party Utilities » SWGAide:
http://www.swgcraft.org/forums/viewforum.php?f=43

Board » Development Forums » *SWGAide Developers*:
http://www.swgcraft.org/forums/viewforum.php?f=40

Sticked: Notes for new developers (tips and starters and this doc):
http://www.swgcraft.org/forums/viewtopic.php?f=40&t=1190


PS: Feel free to edit and update this document / Zimoon