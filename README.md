# ipa-sdk

Script to upload data set into IPA from an IPA URL Integration module formatted HTML file.

To build:
    
    mvn assembly:single

To run:
    
    java -cp .:target/ipa-sdk-jar-with-dependencies.jar <ipaUserEmail> <ipaPassword> <integrationApiFormattedFile.html>
