## Build


```
./mvnw clean package -DskipTests=true
```


### Deploy to Cloud Foundry

```
cf push
```

> If app name conflicts, use `--random-route` option.

```
$ curl -u username:password -H "X-Broker-API-Version: 2.13" https://demo-osbapi.cfapps.io/v2/catalog
{"services":[{"id":"b98aea8a-9961-44bc-b68e-627b5d495a94","name":"demo","description":"Demo","bindable":true,"planUpdateable":false,"requires":[],"tags":["demo"],"metadata":{"displayName":"Demo","documentationUrl":"https://twitter.com/making","imageUrl":"https://avatars2.githubusercontent.com/u/19211531","longDescription":"Demo","providerDisplayName":"@making","supportUrl":"https://twitter.com/making"},"plans":[{"id":"dcb86c66-274e-44c0-941d-d78cacd12ccc","name":"demo","description":"Demo","free":true,"metadata":{"displayName":"Demo","bullets":["Demo"],"costs":[{"amount":{"usd":0},"unit":"MONTHLY"}]}}]}]}
```

### Enable Service Broker on Cloud Foundry

#### Register the service broker

If you have an admin privilege, use following commands:

```
cf create-service-broker demo username password https://demo-osbapi.cfapps.io
cf enable-service-access demo
```

> If you are not an admin, run `cf create-service-broker demo username password https://demo-osbapi.cfapps.io --space-scoped` instead.

```
$ cf marketplace
Getting services from marketplace in org ikam / space home as admin...
OK

service     plans     description
demo        demo      Demo
```

#### Create a service instance

```
cf create-service demo demo hello
```

#### Create a service key

```
cf create-service-key hello hello-key
```

Show the service key

```
$ cf service-key hello hello-key
Getting key hello-key for service instance hello as admin...

{
 "password": "f78538ee-2c5f-48d5-b519-7da00066a657",
 "username": "f59be4ad-27ba-4b71-86df-5da7b66489f0"
}
```