## Build


```
./mvnw clean package -DskipTests=true
```


### Deploy to [Pivotal Web Services](https://run.pivotal.io)

```
cf push
```

> If app name conflicts, use `--random-route` option.

```
$ curl -u username:password -H "X-Broker-API-Version: 2.13" https://demo-osbapi.cfapps.io/v2/catalog
{"services":[{"id":"b98aea8a-9961-44bc-b68e-627b5d495a94","name":"demo","description":"Demo","bindable":true,"planUpdateable":false,"requires":[],"tags":["demo"],"metadata":{"displayName":"Demo","documentationUrl":"https://twitter.com/making","imageUrl":"https://avatars2.githubusercontent.com/u/19211531","longDescription":"Demo","providerDisplayName":"@making","supportUrl":"https://twitter.com/making"},"plans":[{"id":"dcb86c66-274e-44c0-941d-d78cacd12ccc","name":"demo","description":"Demo","free":true,"metadata":{"displayName":"Demo","bullets":["Demo"],"costs":[{"amount":{"usd":0},"unit":"MONTHLY"}]}}]}]}
```

In this instruction, we use this service broker deployed on PWS, but it doesn't matter where to deploy.

### Enable Open Service Broker on Cloud Foundry

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

Check the service instance

```
$ cf services
Getting services in org ikam / space home as admin...
OK

name       service        plan     bound apps    last operation
hello      demo           demo                   create succeeded
```

#### Create a service binding (service key)

```
cf create-service-key hello hello-key
```

Check the service bindings

```
$ cf service-keys hello
Getting keys for service instance hello as admin...

name
hello-key
```

Let's see the detail,

```
$ cf service-key hello hello-key
Getting key hello-key for service instance hello as admin...

{
 "password": "f78538ee-2c5f-48d5-b519-7da00066a657",
 "username": "f59be4ad-27ba-4b71-86df-5da7b66489f0"
}
```

### Enable Open Service Broker on Kubernetes

#### (Option) Start minikube

ensure that you are using version v0.23.1 or above 

```
minikube start --extra-config=apiserver.Authorization.Mode=RBAC --memory=4096

kubectl create clusterrolebinding tiller-cluster-admin \
    --clusterrole=cluster-admin \
    --serviceaccount=kube-system:default
```

#### Install Service Catalog

```
helm init
helm repo add svc-cat https://svc-catalog-charts.storage.googleapis.com

helm install svc-cat/catalog --name catalog --namespace catalog --set insecure=true
```

> References: https://github.com/mattmcneeney/osbapi-demo/blob/master/scripts/k8s/setup.sh


Check if Service Catalog is working

```
              DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
deploy/catalog-catalog-apiserver            1         1         1            1           3m
deploy/catalog-catalog-controller-manager   1         1         1            1           3m

NAME                                               DESIRED   CURRENT   READY     AGE
rs/catalog-catalog-apiserver-b6bc6c698             1         1         1         3m
rs/catalog-catalog-controller-manager-675c4b7c65   1         1         1         3m

NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
deploy/catalog-catalog-apiserver            1         1         1            1           3m
deploy/catalog-catalog-controller-manager   1         1         1            1           3m

NAME                                               DESIRED   CURRENT   READY     AGE
rs/catalog-catalog-apiserver-b6bc6c698             1         1         1         3m
rs/catalog-catalog-controller-manager-675c4b7c65   1         1         1         3m

NAME                                                     READY     STATUS    RESTARTS   AGE
po/catalog-catalog-apiserver-b6bc6c698-68tjl             2/2       Running   0          3m
po/catalog-catalog-controller-manager-675c4b7c65-fn6nf   1/1       Running   2          3m

NAME                            TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)         AGE
svc/catalog-catalog-apiserver   NodePort   10.109.180.16   <none>        443:30443/TCP   3m
```

#### Register the service broker

```
kubectl apply -f k8s/secret.yml
kubectl apply -f k8s/service-broker.yml
```

Check the service broker

```
$ kubectl get clusterservicebrokers 
NAME      AGE
demo      3m
$ kubectl get clusterserviceclasses
NAME                                   AGE
b98aea8a-9961-44bc-b68e-627b5d495a94   1m
$ kubectl get clusterserviceplans
NAME                                   AGE
dcb86c66-274e-44c0-941d-d78cacd12ccc   1m
```

> If you want to see the details, add `-o yaml` option


#### Create a service instance

```
kubectl apply -f k8s/service-instance.yml 
```

Check the service instance

```
$ kubectl get serviceinstances
NAME      AGE
hello     34s
```

Let's see the detail,

```
$ kubectl get serviceinstances -o yaml
apiVersion: v1
items:
- apiVersion: servicecatalog.k8s.io/v1beta1
  kind: ServiceInstance
  metadata:
    annotations:
      kubectl.kubernetes.io/last-applied-configuration: |
        {"apiVersion":"servicecatalog.k8s.io/v1beta1","kind":"ServiceInstance","metadata":{"annotations":{},"name":"hello","namespace":"default"},"spec":{"clusterServiceClassExternalName":"demo","clusterServicePlanExternalName":"demo"}}
    creationTimestamp: 2017-12-11T18:03:49Z
    finalizers:
    - kubernetes-incubator/service-catalog
    generation: 1
    name: hello
    namespace: default
    resourceVersion: "14"
    selfLink: /apis/servicecatalog.k8s.io/v1beta1/namespaces/default/serviceinstances/hello
    uid: a3e6f62c-de9d-11e7-b168-0242ac11000a
  spec:
    clusterServiceClassExternalName: demo
    clusterServiceClassRef:
      name: b98aea8a-9961-44bc-b68e-627b5d495a94
    clusterServicePlanExternalName: demo
    clusterServicePlanRef:
      name: dcb86c66-274e-44c0-941d-d78cacd12ccc
    externalID: 8bc979bd-7793-4677-8a32-fae9c14eef9c
    updateRequests: 0
  status:
    asyncOpInProgress: false
    conditions:
    - lastTransitionTime: 2017-12-11T18:03:51Z
      message: The instance was provisioned successfully
      reason: ProvisionedSuccessfully
      status: "True"
      type: Ready
    dashboardURL: http://example.com
    deprovisionStatus: Required
    externalProperties:
      clusterServicePlanExternalID: dcb86c66-274e-44c0-941d-d78cacd12ccc
      clusterServicePlanExternalName: demo
    orphanMitigationInProgress: false
    reconciledGeneration: 1
kind: List
metadata:
  resourceVersion: ""
  selfLink: ""
```

### Create a service binding

```
kubectl apply -f k8s/service-binding.yml 
```

Check the service binding

```
$ kubectl get servicebindings
NAME        AGE
hello-key   18s
```

Let's see the detail,

```
$ kubectl get servicebindings -o yaml
apiVersion: v1
items:
- apiVersion: servicecatalog.k8s.io/v1beta1
  kind: ServiceBinding
  metadata:
    annotations:
      kubectl.kubernetes.io/last-applied-configuration: |
        {"apiVersion":"servicecatalog.k8s.io/v1beta1","kind":"ServiceBinding","metadata":{"annotations":{},"name":"hello-key","namespace":"default"},"spec":{"instanceRef":{"name":"hello"},"secretName":"hello-key-secret"}}
    creationTimestamp: 2017-12-11T18:10:00Z
    finalizers:
    - kubernetes-incubator/service-catalog
    generation: 1
    name: hello-key
    namespace: default
    resourceVersion: "19"
    selfLink: /apis/servicecatalog.k8s.io/v1beta1/namespaces/default/servicebindings/hello-key
    uid: 808d7717-de9e-11e7-b168-0242ac11000a
  spec:
    externalID: 79f1ee77-9121-49b6-bf9b-cb08feb68ce8
    instanceRef:
      name: hello
    secretName: hello-key-secret
  status:
    asyncOpInProgress: false
    conditions:
    - lastTransitionTime: 2017-12-11T18:10:01Z
      message: Injected bind result
      reason: InjectedBindResult
      status: "True"
      type: Ready
    externalProperties: {}
    orphanMitigationInProgress: false
    reconciledGeneration: 1
    unbindStatus: Required
kind: List
metadata:
  resourceVersion: ""
  selfLink: ""
```

You can see the credentials in the `hello-key-secret`

```
$ kubectl get secret hello-key-secret  -o yaml
apiVersion: v1
data:
  password: NDk5ODgxNWUtZTE4OC00YmM2LTk4Y2ItYjQzNDNjYzc3MWUw
  username: OWM4MGNlMzctNjMxMy00YzEyLTk3YTctMmZmN2M1YjkzZWU4
kind: Secret
metadata:
  creationTimestamp: 2017-12-11T18:10:01Z
  name: hello-key-secret
  namespace: default
  ownerReferences:
  - apiVersion: servicecatalog.k8s.io/v1beta1
    blockOwnerDeletion: true
    controller: true
    kind: ServiceBinding
    name: hello-key
    uid: 808d7717-de9e-11e7-b168-0242ac11000a
  resourceVersion: "71795"
  selfLink: /api/v1/namespaces/default/secrets/hello-key-secret
  uid: 81746e0a-de9e-11e7-a13a-ba1d6b67ce25
type: Opaque
```

Decode base64 encoded credentials

```
$ echo NDk5ODgxNWUtZTE4OC00YmM2LTk4Y2ItYjQzNDNjYzc3MWUw | base64 -D
4998815e-e188-4bc6-98cb-b4343cc771e0 
$ echo OWM4MGNlMzctNjMxMy00YzEyLTk3YTctMmZmN2M1YjkzZWU4 | base64 -D
9c80ce37-6313-4c12-97a7-2ff7c5b93ee8
```