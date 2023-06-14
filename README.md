
run program with a mandatory parameter, which a property configuration file (include path). 
Below is a template:

------begin template-------
clientId:xxxxx
username:xxx@abc.com
nCinoPrivateKeyPath:
tokenEndpoint:https://test.salesforce.com/services/oauth2/token
nCinoInstanceUrl: https://abo--devpoc1.sandbox.my.salesforce.com
subscribedChangeEvents:  LLC_BI__Connection__ChangeEvent, LLC_BI__Collateral__ChangeEvent, LLC_BI__Account_Covenant__ChangeEvent
relayFrom:
gcsBuckName:jianliu888-hometest
bigQueryDatasetName:jianliuhometest
keystoreType:JKS
keystorePath:./keystore.jks
keystoreAlias:myalias
googleCredentialKeyPath:
----end template------

How to run:
java -cp "target/Orchestrator-agent-1.0-SNAPSHOT-phat.jar" com.jil.App localConfig/config.properties

localConfig/config.properties is a program parameter for your properties configuration file.