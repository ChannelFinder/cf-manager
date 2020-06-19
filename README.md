# cf-manager

A project to consilidate some general channelfinder testing, debugging, and reporting operations

### Building

`mvn clean install`

### Launching cf-manager

```
cd /target/
java -jar cf-manager-0.0.1-SNAPSHOT.jar -h
```

Supported operations

```
-generate-report          - Generate a report on the recsync properties
-es_host  localhost       - elastic server host
-es_port  9200            - elastic server port
-help                     - print this text

```




### Syntax Rules for the Standard name format

The default structure of NSLS2 pv names is as follows:
	
`PSy:PI-SSy:SI-TSy:TI{PDev:PDI-SDev:SDI-...}Sg:SgI-SD`

The name is composed of system, device, and signal information.


```
1		Characters in names to conform with allowed EPICS conventions				
		Allowed characters			a-z  A-Z 0-9 _ - : [ ] < > ; {}	
						
2		Reserved delimiters (not to be used in name elements)				
		{		System{Device delimiter		
		}		Device}Signal delimiter		
		:		Precedes instance when used		
		-		delimiter  system  Primary-Secondary-Tertiary, device-subdevice		
		-		delimiter  signal instance-domain		
						
3		Total name length				
		60 characters including delimeters				
						
4		Name taxonomy				
		Name consists of parts which consist of elements				
						
5		Required name elements				
	a.	As a guide, name should include only the parts and elements needed				
		to provide a unique identifier that meets the need for a name.		
						
	b.	If a name is needed the Device part is the required minimum name.				
						
	c.	Within parts the minimum requirement is:				
		System part		Primary system name		
		Device part		None (if system adequately describes the source of the signal)		
		Signal part		Signal domain		
						
	d.	If the system part is used, the elements must be added in order as required 				
		to ensure unique identification of the name.				
		Primary-Secondary			Allowed	
		Primary-Tertiary			Forbidden	
```

#### The validators run by the generate-report process are the following:

##### 1. Name length validator:

Check that pv names don't exceed 60 characters including delimiters.

##### 2. Device validator:

Check that the pv names contain a device and the primary device is in the approved device_name list
 
##### 2. System validator:
 
Check that the pv names contain a primary system defined in the approved system_name list