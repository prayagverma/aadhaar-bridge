# Aadhaar Bridge

*Public Repository for Aadhaar Bridge.*

The components of the code section are divided into different sections/folders namely 

* *libraries*
	* This folder contains the below mentioned:
		* **Aadhaar Bridge APK**<br />
		This app will be doing PID encryption and will be capturing the raw biometrics in the form of fingerprint and iris once the corresponding intent is called. 
		* **Aadhaar Bridge KUA Gate 0.0.1**<br />
		This comprises of a gateway jar embedded with tomcat server along with the application.properties file. Requests from the client will be forwarded to the gateway which in turn will be directed to the Aadhaar Bridge Service.
		* **Aadhaar Bridge AUA Capture Wire**<br />
		Jar file will contain all the POJO's required in order to compose the request which can be either Auth or OTP or e-Kyc. 
		* **Aadhaar Bridge AUA Packet Creator**<br />
		Jar file will be responsible to convert the request JSON to PID Encrypted JSON as specified by the UIDAI standards
		
* *documents*<br />
Draft agreements that the Sub-AUA needs to sign in order to enroll for the Aadhaar Bridge Auth or Aadhaar Bridge Auth and e-KYC Services respectively.

* *examples*
	* This folder contains the below mentioned
		* **Aadhaar Bridge Sample App**<br />
		Sample app that will let the user do Demographic Authentication, Biometric Authentication, OTP generation, OTP based authentication cum verification and KYC using Fingerprint and Iris.  
		   		
		
#### *Kindly look at the wiki for the better understanding of the different aspects of Aadhaar Bridge as a SaaS product. 		

