SWGAide_README.txt


Installation
============

Create a new folder for SWGAide, anywhere you want.

Drag and drop the SWGAide.jar file in the new folder.

Double click the icon for SWGAide.jar to launch it.

SWGAide guides you through its initialization. Actually, you just have to
verify (or possibly find) the folder for StarWarsGalaxies; you cannot make
a useful installation of SWGAide without SWG.

SWGAide creates several folders and files its directory; they are all self
explanatory.



ATTENTION: For Windows 7 and Vista, see important section further down.



Usage
=====

Begin to just play around; try to right click at various GUI elements.

When you have a question, press F1 at the keyboard, or select "Help". 

The Main panel's left-hand side displays
1) stations (accounts)
2) galaxies at which you have a character, or have had
3) files such as aliases, macros, and notes files

You can only see characters who have saved mails. To be able to read game-mails
in SWGAide you must, in-game and for each character, execute the command,

    /mailsave

After some time a SWG system message tells how many mails that were saved.
The more mails to save the longer delay as they are retrieved on demand one by
one. Thus, retain only important mails in game, delete the rest. Once they are
saved to disk it is safe to delete in-game mails. 

NOTICE: If the system message about saved mails never shows up then there are
some old or corrupt mails which the in-game mail client cannot retrieve. In SWG,
open the mail client, select a mail one at a time beginning with the oldest. If
the mail body never shows up, delete the mail since it is corrupt at the server.
Continue until the system message shows up. Or, if you have no valuable mails,
use the command /emptyMail

NOTE: when you edit a notes files, you cannot have the very same file open
in-game while editing it outside of game. The outside edits are overwritten by
the game when you close the notepad.

Again, when you have questions about something in SWGAide, navigate to the
related panel and press F1, or select Help. Help is context sensitive. During
initiation all help files are copied to a "docs" folder as web pages.



SWGAide is Open Source
======================

Java developers and also non-programmers are warmly welcomed. 
The latter group could gather and submit all kinds of data that could be useful
in SWGAide and post that at the SWGAide forum at SWGCraft.org. Or anything else
that is useful for other players, at any format or syntax.
Developers are welcomed to download the source code from SourceForge.net,
browse the code but PM me before going ahead to implement stuff.



The file SWGAide.DAT
====================

This file contains all of your settings and whatever you have stored in SWGAide,
such as resource inventory, comments at schematics, etc. You cannot edit this
file, it is a "serialized Java object stream". SWGAide creates a backup of this
file regularly but also about every ten minute. If SWGAide complains that the
file is corrupt you have three options:

1) Exit SWGAide and manually replace the corrupt file with one of the recent
   backup files. This gives you a chance to copy files to a safe place, or to
   try files other than the most recent, etc.
2) Let SWGAide try the file SWGAide.BAK from the backup folder.
3) Report the issue at SWGCraft.org in the SWGAide forum.



Error: File not found
=====================

If SWGAide reports numerous errors to the error log file, all saying 'file not
found', then you are most probably using Windows 7 or Vista >> see next section.
Otherwise, post at the SWGAide forum at SWGCraft.org and describe what happens
and what you believe the reason is.

DO NOT post the log file at the forum, it may contain sensitive data.



Windows 7 and Windows Vista
===========================

If you are using Windows 7 or Vista you must adjust the preferences so that
SWGAide can run in "administrator" mode. Notice that this is not an error with
SWGAide; a JAR file does not in itself execute anything but it is just read by
the Java runtime engine (JRE).

1) Find the Java installation and a file named "javaw.exe" (notice the 'w'),
   usually somewhere similar to "C:\Program Files\Java\jre6\bin\javaw.exe".
2) Right-click the icon for javaw.exe and select properties.
3) Select Compatibility.
4) Find and select "Run this program as an administrator" to let javaw.exe
   execute all Java applications in administrator mode.
5) Launch SWGAide with a double click at its icon.

IMPORTANT: This must be repeated each time you upgrade the Java JRE.

Unless you have disabled UAC (User Access Control) you will be prompted with a
dialog each time. It is not necessary to disable UAC to run SWGAide.

NOTICE: You may launch SWGAide anyway, but many features will emit errors that
are written to SWGAide's log files and possibly crash SWGAide which in worst
case may corrupt the DAT file with your preferences and content.



Launching problems
==================

Some users have had other problems starting SWGAide. Try these steps and
after each step try to double-click the icon to determine if it was successful,
otherwise continue with next step.

1) Is the most recent version of Java installed? Try to uninstall all previous
   versions of Java at Window's "Control Panel >> Add/Remove Applications" and
   then install the most recent version from http://java.sun.com, just the JRE
   is necessary, not JDK or anything else.

2) Verify that Java can execute JAR files. Open a "Command Prompt" (the arcane
   DOS-looking terminal) and navigate to the folder with SWGAide by the help of
   commands such as

       cd  "C:\Program Files\SWGAide"

   if you created a folder named "SWGAide" within "Program Files".
   Type the following command to try to launch SWGAide

       java -jar SWGAide.jar

   If this did not work, verify whether the Java installation works with any 
   other Java applications; there may be a problem with Windows or your Java 
   installation. Otherwise, if it worked but double clicking does not work, 
   continue with next step.

3) Make Windows properly support Java JAR files (this is in Windows XP):

   Navigate to My Computer -> Tools -> Folder Options -> File Types.
   Find file extension "JAR":
       If JAR is not found:
           Create a new type for JAR.
           Select JAR (the new file extension) and continue.
   Select JAR and click Advanced:
       Select Edit.
       Verify that the program path and trailing characters look similar to 
       this example, the exact look depends on your system:

           "C:\Program Files\Java\jre6\bin\javaw.exe" -jar "%1" %*

       Be very careful with all the quotes and other signs.
       Verify that "Use DDE" is selected.
       Verify that "Application" reads "javaw" with a trailing 'w'.



Errors and problems
===================

All reports, requests, criticism, or whatever you report is warmly welcomed. 
Use the forum for SWGAide at http://www.swgcraft.org/forums/viewforum.php?f=43

Errors and messages are logged to files in the folder "logs":
1) Open a new mail
2) Attach the log file
3) Attach the error log file <=== this is IMPORTANT
4) Describe what you were doing, the more details the better
5) Describe how to reproduce the error, if that is possible
6) Mail me the log files and your information to simongronlund ((O)) gmail com
The better description the quicker the root cause is found and fixed.

The two log files can always be deleted at your leisure.

Again, DO NOT post log files at the forum, it may contain sensitive data.


Uninstall
=========

SWGAide does not add anything to the Windows Registry and it does not spread
files all over your computer. SWGAide is contained within the folder you created
for SWGAide. Save the "mails" folder and other files which you want to keep and
then delete SWGAide's folder ... and done.



Enjoy

Zimoon @ Chimaera
