# ipa-sdk

Script to upload data set into IPA from an IPA URL Integration module formatted HTML file.

To build:
    
    mvn package assembly:single

To run:
    
    java -jar target/ipa-sdk-jar-with-dependencies.jar \
      <proxyHost:port|none> <ipaUserEmail> <ipaPassword> <integrationApiFormattedFile.html>
