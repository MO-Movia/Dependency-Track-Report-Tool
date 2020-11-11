
# Third Party Audit & License Validation Utility (TPALVU)

### Download Source
 - git clone https://github.com/MO-Movia/Dependency-Track-Report-Tool.git  
 - cd *Dependency-Track-Report-Tool*
 
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

## Configuration
From the cloned folder edit *config.properties*

 1. set the value of **REST_API_KEY**=*Prerequiste#9* 
 2. set the value of **REST_API_PID**=*Prerequiste#8*

## Build
From the cloned folder run
*mvn clean compile package -DskipTests*

 - *utility.tpalv-1.0.0.jar* shall be generated in the *target* folder
 - Copy *utility.tpalv-1.0.0.jar* to a new folder outside the project folder for testing.
 - Copy *config.properties* to the above folder.

## Run
java -jar utility.tpalv-1.0.0.jar

## Parameters
Usage: *java -jar utility.tpalv-1.0.0.jar* *[-configurations]*
where each configuration is in the format:
> -*configuration*=*value*

and separated by a space.

In order to overrride the configurations in the config.properties to running the tool use command line arguments like this:

**java -jar utility.tpalv-1.0.0.jar -U=http://localhost:8080 -A=path-to-movia_approved_licenses.txt**

Available configurations are:
| Configuration | Description |
|--|--|
| Dependency Track URL | Dependency Track Rest API URL |
| Dependency Track API Key | Dependency Track Rest API Key |
| Dependency Track Project ID | Dependency Track Project ID|
| Approved License File | File path of the Approved Licenses  |
| License Translation File | File path of the License Translations  |
| White List File | File path of the White list |
| Audit Report | File path of the Audit report csv |
| License List | File path of the unique License list  |
| License Text | File path of the License Text|

> File path can include Jenkins' environment variables (global, project or node specific). 
> Environment variables MUST be in this format:  
> &nbsp;&nbsp;&nbsp;&nbsp;&#36;&#123;*<ENV_VARIABLE>*&#125;
> Multiple variables can be combined like this:  
> &nbsp;&nbsp;&nbsp;&nbsp;&#36;&#123;*<ENV_VARIABLE_0>*&#125;&#36;&#123;*<ENV_VARIABLE_1>*&#125;
> If just file names are provided, it shall be picked or stored from Jenkins' root directory.

The relevant configuration can set in Jenkins for output files are as follows:
Assumptions:
Jenkins Project Name : MOVIA-CORE
JENKINS_HOME : C:\Users\User_Name\.jenkins
BASE : C:\Program Files\Jenkins  ( you can find this path in Manage Jenkins > System Information > Environment Variables > BASE )
OS : Windows 8
Plugin installed via Manage Jenkins > Manage Plugin > Upload Plugin
Project configured with **Add build step** - " Third Party Audit & License Validity".
Project Build Number is : 12
| Output Configuration | File Name| Output File Path
|--|--|--|
| Audit Report | movia_audit_out.csv |C:\Program Files\Jenkins\movia_audit_out.csv
| License List | ${WORKSPACE}\movia_license_list.csv |C:\Users\User_Name\.jenkins\workspace\MOVIA-CORE\movia_license_list.csv
| License Text | ${WORKSPACE}\ ${BUILD_NUMBER}-movia_license_text.txt| C:\Users\User_Name\.jenkins\workspace\MOVIA-CORE\ 12-movia_license_text.txt
