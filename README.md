# Spaß
A Mock Service Server over HTTP


## Description
This is a standalone server which mocks HTTP communication for testing use.
1. SOAP
1. REST
1. Classic GET

## name origin
Spaß /ʃpaːs/

↑

Mozart(1787), _Ein Musikalischer Spaß_

↑

mocquere

late Middle English: from Old French mocquer ‘deride’; Modern French ‘mocque’ /mɔ.ke/

↑

mock


### Features

#### MVP feautures
1. It responds to a request with a fixed XML.

#### Features
1. SOAP mock
    1. It responds with an XML put in a SOAP response directory if a request is identical to an XML in a SOAP request directory except for \s (whitespaces / line breaks) between tags.
        1.  It responds with a default XML put in a SOAP response directory if a request is not identical. This is the same feature of MVP's.
1. REST mock
1. classic GET mock with query strings


## Preparation
1. Install sbt
    - via official site [sbt download](https://www.scala-sbt.org/download.html), [Scoop](https://scoop.sh/) for Windows or [Home Brew](https://brew.sh/) for macOS.
1. To build this app you need the Internet in order to download libraries.
1. Clone this repo.


## How to run

```bash
$ cd [cloned directory]
```

### by default port 9000
http://localhost:9000/
- Dev Mode for browser-reload mode:
```bash
$ sbt run
```

### changing the port
e.g. 8080
http://localhost:8080/
- Dev Mode for browser-reload mode:
```bash
$ sbt "run 8080"
```


## Settings
### SOAP
- `./mapping/soap/requests`
    - `001.xml` ←sample
    - Put here an expected request XML. Several files are allowed.
- `./mapping/soap/responses`
    - `001.xml` ←sample
    - `default.xml`
    - Put here an expected response XML. Several files are allowed.

### REST
- `./mapping/rest/[type name]/`
    - `001.xml` ←sample
    - Put here an expected request XML. Several files are allowed.
    - For instance requesting `http://localhost:9000/rest/users/001` seeks a response in `./mapping/rest/users/001.xml`

### Config
`mapping` directory is changeable.
Configure the following key in `./conf/application.conf`
```conf
spass.mapping.rootpath=C:\\mock\\mapping
```


## Usage

### SOAP
http://localhost:9000/soap
- When posing to the URI above, it searchs and returns a corresponding XML.
1. It checks if the same XML in `./mapping/soap/requests` exists as one in a request.
1. It tries exact match with `*.xml` and then tries RegEx match with `*.regex`.
1. If they are matched, it returns an XML in `./mapping/soap/responses` with the same filename as in the request directory.
1. If they not are matched, it returns `./mapping/soap/responses/default.xml`.

### REST
http://localhost:9000/rest
1. When requesting GET /[type name]/id (e.g. http://localhost:9000/rest/users/001) it searches and returns an XML/JSON in the same directory structure as a requested URI.

### Classic GET
http://localhost:9000/classic?[key]=[value](&...)
- When getting to the URI above, it searchs and returns a corresponding XML, at present not a JSON.
1. When requesting GET with query strings or even with a deep link /[type name]/id (e.g. http://localhost:9000/rest/users/001) it searches parameter files `*.conf` and tries to match queries between a request and an expection in `./mapping/classic/get[/...]/requests`.
1. It returns a matched response.

