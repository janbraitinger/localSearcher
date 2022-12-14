
[![Generic badge](https://img.shields.io/badge/version-0.5-orange.svg)](https://shields.io/)
[![Generic badge](https://img.shields.io/badge/maintaining-yes-green.svg)](https://shields.io/)
[![made-with-java](https://img.shields.io/badge/Made%20with-Java-1f425f.svg)](https://www.java.com)
[![made-with-java](https://img.shields.io/badge/Made%20with-Node.js-1f425f.svg)](https://www.nodejs.com)


# Intelligent searching for (multiple-) terms in unstructured documents using Apache Lucene and Word Embeddings.
<img src="screenshot.png" style="border-radius:10px;">

<br/><br/>

### The search engine provides a API for receiving requests. Results are available in JSON format. 
```console
GET localhost:4001/status
```
```console
GET localhost:4001/search/{searchQuery}:{embeddingIDs}
```
```console
GET localhost:4001/conf
```
```console
GET localhost:4001/setconf/{documetFolder}
```
<br/>

### The webserver builds on node.js and runs on localhost:3000
 ```console
 $ cd src/webserver/index.js
 $ npm install 
 $ node index.js
```


<br/><br/>
Contact: jan.braitinger@uni-ulm.de
