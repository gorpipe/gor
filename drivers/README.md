# Drivers README 

This is the read me file for the drivers module.

## Setup

For the driver tests to run several secrets need to be set.  The secrets are stored in [Secret Server](https://secretserver.wuxinextcode.com/SecretServer/SecretView.aspx?secretid=4153)

The content can either be copied to `tests/config/secrets.env` file or used to set the necessary environment variables.
         
Tests that access the cloud should be marked as IntegrationTest.
      
### Access variables for S3 testing
#### Three keys sets for AWS S3.
```
S3_KEY
S3_SECRET
```
                                  
#### Access keys for S3 compatiple storage at Google cloud.
```
S3_GOOGLE_KEY
S3_GOOGLE_SECRET
```
  
### Access variables for Google Cloud
```
GS_CLIENT_ID
GS_SECRET
GS_REFRESH_TOKEN
```

### Access variables for OCI S3 compatibility
```
S3_COMPAT_KEY
S3_COMPAT_SECRET
S3_COMPAT_ENDPOINT
```

### Access variables for OCI
```
OCI_TENANT
OCI_USER

OCI_AUTH_TOKEN_KEY_FILE
OCI_AUTH_TOKEN_KEY
OCI_AUTH_TOKEN_JWT

OCI_SIMPLE_PRIVATE_KEY
OCI_SIMPLE_FINGERPRINT
```

### secrets.env File
The tests read config from the  `tests/config/secrets.env` if it exists.
