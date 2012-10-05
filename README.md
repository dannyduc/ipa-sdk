# ipa-sdk

Script to upload data set into IPA from an IPA URL Integration module formatted HTML file.

To build:
    
    mvn package assembly:single

To run:
    
    java -cp target/ipa-sdk-jar-with-dependencies.jar \
      com.ingenuity.ipa.sdk.uploader.IpaUploaderApp \
      <ipaUserEmail> <ipaPassword> <integrationApiFormattedFile.html>
