
[![Generic badge](https://img.shields.io/badge/version-0.5-orange.svg)](https://shields.io/)
[![Generic badge](https://img.shields.io/badge/maintaining-yes-green.svg)](https://shields.io/)
[![made-with-java](https://img.shields.io/badge/Made%20with-Java-1f425f.svg)](https://www.java.com)
[![made-with-java](https://img.shields.io/badge/Made%20with-Node.js-1f425f.svg)](https://www.nodejs.com)


# The system employs advanced techniques such as Apache Lucene and Word Embeddings to perform intelligent searching of multiple terms within unstructured documents.
<img src="screenshot.png" style="border-radius:10px;">

<br/><br/>

## First of all: Set up `conf.ini` 

<br/><br/>

## Running WebApplication
 ```console
 $ cd src/webserver/index.js
 $ npm install 
 $ node index.js
```
You can access it on the browser by typing `localhost:3000`.
<br/><br/>

### Searching is only possible if the cirlce on the left top corner is green.

## Running Search Engine
 ```console
 $ cd src/luceneSearchEngine
 $ mvn compile
 $ mvn exec:java -Dexec.mainClass=lucene.searchEngine.Main
```
Note that it may take up to 3 minutes for the engine to start.
<br/><br/>

### In the top right corner, there is a settings symbol. By clicking on it, you can set a new directory where the documents should be indexed.

## 
<br/><br/>
<hr/>
<br/>

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

<br/><br/>
Contact: jan.braitinger@uni-ulm.de
