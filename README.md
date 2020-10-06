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
| -U | Dependency Track Rest API URL |
| -K | Dependency Track Rest API Key |
| -I | Dependency Track Project ID|
| -A | File path of the Approved Licenses  |
| -X | File path of the License Translations  |
| -W | File path of the White list |
| -R | File path of the Audit report csv |
| -L | File path of the unique License list  |
| -T | File path of the License Text|
| -? | print help message  |