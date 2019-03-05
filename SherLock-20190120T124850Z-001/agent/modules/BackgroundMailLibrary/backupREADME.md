Background Mail Library 1.8
=====================
[ ![Download](https://api.bintray.com/packages/kristijandraca/maven/BackgroundMailLibrary/images/download.svg) ](https://bintray.com/kristijandraca/maven/BackgroundMailLibrary/_latestVersion)[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-BackgroundMailLibrary-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/276)   
Android Library for sending mail in background

**How to use it**

            BackgroundMail bm = new BackgroundMail(context);
            bm.setGmailUserName("yourgmail@gmail.com");
            bm.setGmailPassword("yourgmailpassword");
            bm.setMailTo("receiver@gmail.com");
            bm.setFormSubject("Subject");
            bm.setFormBody("Body");
            bm.send();
            
**Permissions**

            <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
            <uses-permission android:name="android.permission.INTERNET"/>
            <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
            
**This is optional**

            bm.setSendingMessage("Loading...");
            bm.setSendingMessageSuccess("Your message was sent successfully.");
            bm.setProcessVisibility(false);
            bm.setAttachment(Environment.getExternalStorageDirectory().getPath()+File.pathSeparator+"somefile.txt");
            
**Changelog**

            -version 1.6
            Added Android Studio project
            -version 1.5
            fix #4
            - version 1.4
            Added attachments support
            - version 1.3
            Added password encryption for better security
            - version 1.2
            Added process visibility feature
            - version 1.1
            Added demo
            Bug fix
            - version 1.0
            First commit

**Credits**

            © 2013-2015 Kristijan Drača     
            
**License**

			Copyright 2015 Kristijan Drača

			Licensed under the Apache License, Version 2.0 (the "License");
			you may not use this file except in compliance with the License.
			You may obtain a copy of the License at

			http://www.apache.org/licenses/LICENSE-2.0

			Unless required by applicable law or agreed to in writing, software
			distributed under the License is distributed on an "AS IS" BASIS,
			WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
			See the License for the specific language governing permissions and
			limitations under the License.


