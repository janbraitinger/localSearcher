
[![Generic badge](https://img.shields.io/badge/version-0.5-orange.svg)](https://shields.io/)
[![Generic badge](https://img.shields.io/badge/maintaining-yes-green.svg)](https://shields.io/)
[![made-with-java](https://img.shields.io/badge/Made%20with-Java-1f425f.svg)](https://www.java.com)
[![made-with-java](https://img.shields.io/badge/Made%20with-Node.js-1f425f.svg)](https://www.nodejs.com)


# The system employs advanced techniques such as Apache Lucene and Word Embeddings to perform intelligent searching of multiple terms within unstructured documents.
<img src="screenshot.png" style="border-radius:10px;">

<br/><br/>

### The search engine offers an Application Programming Interface (API) that adheres to the architectural principles of Representational State Transfer (REST), through which it receives requests. The output of said requests is presented in the JavaScript Object Notation (JSON) format.
```console
GET localhost:4001/api/v1/status
```
```console
GET localhost:4001/api/v1/search?data={"query":"YourSearchQuery"}
```
```console
GET localhost:4001/api/v1/conf
```
<br/>

### The web server, utilizing Node.js as its foundational technology, operates on the local host at port 3000.
 ```console
 $ cd src/webserver/index.js
 $ npm install 
 $ node index.js
```


<br/><br/>
Contact: jan.braitinger@uni-ulm.de
