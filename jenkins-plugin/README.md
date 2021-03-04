
# Third Party Audit & License Validation Jenkins Plugin (TPALVU)

### Download Source
 - git clone https://github.com/MO-Movia/Dependency-Track-Report-Tool.git  
 - cd *Dependency-Track-Report-Tool/jenkins-plugin*
 
## Prerequisite
 - Java 8 u162 (or higher), but not Java 9+
 - Dependency-Track
	 1. Download dependency-track-embedded.war from
    https://github.com/DependencyTrack/dependency-track/releases
	 2. Run Dependency-Track - https://docs.dependencytrack.org/getting-started/initial-startup/ (*java -Xmx8G -jar dependency-track-embedded.war*). This might take some time memory utilization is huge.
	 3. Make sure Dependency-Track by accessing *http://localhost:8080/*
	 4. Follow https://docs.dependencytrack.org/getting-started/initial-startup/ and change password at first login.
	 5. npm install -g @cyclonedx/bom
	 6. Generate a project's bom file. Run *cyclonedx-bom* from a project's root folder. Let's say *mo-licit*. This shall generate *bom.xml* in the project's root folder.
	 7. Create a project in Dependency-Track - *Home > Projects > Create Project*
	 8. Note the URL (something like *http://localhost:8080/projects/588d64a8-a208-4d5f-b3f0-1288acd5ee5a*) to get the project ID. Last part is the project ID, here in the example it is  *588d64a8-a208-4d5f-b3f0-1288acd5ee5a*
	 9. Get API Keys from Dependency Track - *Home > Administration > Access Management > Teams > Automation > API Keys*
	 10. Home > Projects > *NewlyCreatedProjectIn#7* > Dependencies > Upload BOM > Upload (*bom.xml from #6*)

## Build
From the cloned folder run
*mvn clean compile package -DskipTests*

 - *modusoperandi-tpalv.hpi* shall be generated in the *target* folder.
 
 **To upload *modusoperandi-tpalv.hpi* to Jenkins follow the below steps:**

## To install and run Jenkins :

1. Download jenkins from https://www.jenkins.io/download/
 Note:  If the *dependency track* is running on *8080* port , then change the port no. of jenkins while installing.
   After the successful installation of jenkins, redirects the localhost:8080 in the browser.
2. Next,Unlock the jenkins using *administrator password* (get the administrator password from the specified path in the Unlock Jenkins page)
3. Next select the "*Install suggested plugins*" options, this shall start to install the plugins, after the installation click on *Continue button*.
4. Create First Admin User and click on continue. Now "*Instance Configuration*" page displays, click on *save & continue* button.
5. Shows *Getting Started* page with "Jenkins is ready!" text, here click on "*Start using Jenkins*" button to show the Jenkins dashboard.
6. Click on "Create a job" link in the dashboard and Enter a name in the filed and *select Freestyle project* to enable the OK button to continue.
7. Click on *Save* button to show the created job in the dashboard.
8. Click to *Jenkins* in the dashboard and select the "*Manage Jenkins*" menu from the left panel.
9. Select the "*Manage plugins*" from the *System configuration*.
10. If the *Third Party Audit and License Validation Plugin* already uploaded, then uninstall it , for that Click on "*Installed*" tab and uninstall the *Third Party Audit and License Validation Plugin* from the list. 
11. Click on "*Advanced*" tab and from the *Upload Plugin* section select  "Choose file" .
12. Here select the *modusoperandi-tpalv.hpi* file from the *target folder* and click the *upload* button.
13. After installed the file restart the jenkins using the checkbox "*Restart Jenkins when installation is complete and no jobs are running*" or directly run the url  *http://localhost:8080/restart/*
14. After login, select the project from the dashboard , and click "*Configure*" menu from the left panel.
15. On the "*Build*" tab, click on "*Add build step*" drop down and select "*Third Party Audit & License Validity*" from the drop down.
16. Fill all the properties in the shown fields and click on *Save* or *Apply* button.
17. Click the "*Build Now*" menu from the left panel to audit the result in the  output folder.
18. From the *Build History*, click on the latest build, and select "*Console Output*" menu from the left panel to show the console log.  


## Parameters

| Parameter | Description | Default Value |
|--|--|--|
| Dependency Track URL |URL of the dependency track  | http://localhost:8080 |
| Dependency Track API Key |API Key from the dependency track  | LPfV2H90mbapj6TWLUV6tgu1PXYThFDi |
| Dependency Track Project ID |Project ID of the project created in the dependency track| 588d64a8-a208-4d5f-b3f0-1288acd5ee5a |
| Approved License File |File path of the *movia_approved_licenses.txt* in the *inputs*  folder | ${JENKINS_HOME}\\plugins\\modusoperandi-tpalv\\inputs\\movia_approved_licenses.txt |
| License Translation File |File path of the *movia_lic_xlate_list.txt* in the *inputs*  folder  | ${JENKINS_HOME}\\plugins\\modusoperandi-tpalv\\inputs\\movia_lic_xlate_list.txt |
| No License Fix File |File path of the *movia_no_lic_fix.txt* in the *inputs*  folder  | ${JENKINS_HOME}\\plugins\\modusoperandi-tpalv\\inputs\\movia_no_lic_fix.txt |
| Audit Report |File path of the *movia_audit_out.csv* in the *outputs*  folder   | ${WORKSPACE}\\outputs\\movia_audit_out.csv |
| License List |File path of the *movia_license_list.csv* in the *outputs*  folder   | ${WORKSPACE}\\outputs\\movia_license_list.csv |
| License Text |File path of the *movia_license_text.txt* in the *outputs*  folder| ${WORKSPACE}\\outputs\\movia_license_text.txt |

**NOTE: Make sure to replace the below default values with a valid one:**
 - **Dependency Track URL**
 - **Dependency Track API Key**
 - **Dependency Track Project ID**
 
 **Make sure the running user has enough read permission for the input files.**
 
<u>Input-Output Configuration Hints:<u>
<u>Assumptions:</u>
 - Jenkins Project Name: MOVIA-CORE 
 - JENKINS_HOME:  C:\Users\User_Name\\.jenkins 
 - WORKSPACE : C:\Users\User_Name\\.jenkins\workspace
 - BASE: C:\Program Files\Jenkins  ( you can find this path in *Manage Jenkins > System Information > Environment Variables > BASE* ) 
 - OS: Windows 8 
 - Plugin installed via Manage *Jenkins > Manage Plugin > Upload Plugin* 
 - Project configured with ***Add build step** - " Third Party Audit & License Validity"*.
 - Project Build Number is : 12

A couple of usage of Input/Output and their expected/resultant paths:
 
| Input/Output Configuration | File Name| Input/Output File Path
|--|--|--|
| Approved License File | ${WORKSPACE}\inputs\movia_approved_licenses.txt |C:\Users\User_Name\\.jenkins\workspace\MOVIA-CORE\inputs\movia_approved_licenses.txt
| Approved License File |  |  If empty, shall be using the (default) value from the config file.
| Approved License File | ..\movia_approved_licenses.txt| C:\Program Files\movia_approved_licenses.txt
| Audit Report | movia_audit_out.csv |C:\Program Files\Jenkins\movia_audit_out.csv
| Audit Report | ${WORKSPACE}\movia_audit_out.csv |C:\Users\User_Name\\.jenkins\workspace\MOVIA-CORE\movia_audit_out.csv
| Audit Report | ${WORKSPACE}\${BUILD_NUMBER}-movia_audit_out.csv| C:\Users\User_Name\\.jenkins\workspace\MOVIA-CORE\12-movia_audit_out.csv
| Audit Report | ..\movia_audit_out.csv| C:\Program Files\movia_audit_out.csv

NOTE: Generated file path shall be in the console logs.