# Third Party Audit & License Validation Jenkins Plugin (TPALVU)

### Download Source
 - git clone https://gitlab.com/firmusoft/mo-lvu.git  
 - cd *mo-lvu/jenkins-plugin*
 
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

 - *plugins.tpalv.hpi* shall be generated in the *target* folder.
 
 **To upload *plugins.tpalv.hpi* to Jenkins follow the below steps:**

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
10. Click on "*Advanced*" tab and from the *Upload Plugin* section select  "Choose file" .
11. Here select the *plugins.tpalv.hpi* file from the *target folder* and click the *upload* button.
12. After installed the file restart the jenkins using the checkbox "*Restart Jenkins when installation is complete and no jobs are running*" or directly run the url  *http://localhost:8080/restart/*
13. After login, select the project from the dashboard , and click "*Configure*" menu from the left panel.
14. On the "*Build*" tab, click on "*Add build step*" drop down and select "*Third Party Audit & License Validity*" from the drop down.
15. Fill all the properties in the shown fields and click on *Save* or *Apply* button.
16. Click the "*Build Now*" menu from the left panel to audit the result in the  output folder.
17. From the *Build History*, click on the latest build, and select "*Console Output*" menu from the left panel to show the console log.  


## Parameters

| Parameter | Description|
|--|--|
| Dependency Track URL |URL of the dependency track  |
| Dependency Track API Key |API Key from the dependency track  |
| Dependency Track Project ID |Project ID of the project created in the dependency track|
| Approved License File |File path of the *movia_approved_licenses.txt* in the *inputs*  folder |
| License Translation File |File path of the *movia_lic_xlate_list.txt* in the *inputs*  folder  |
| White List File |File path of the *movia_white_list.txt* in the *inputs*  folder  |
| Audit Report |File path of the *movia_audit_out.csv* in the *outputs*  folder   |
| License List |File path of the *movia_license_list.csv* in the *outputs*  folder   |
| License Text |File path of the *movia_license_text.txt* in the *outputs*  folder|

Note : Expect the parent folder only for the outputs. No need to create movia_audit_out.csv, movia_license_list.csv, movia_license_text.txt files.