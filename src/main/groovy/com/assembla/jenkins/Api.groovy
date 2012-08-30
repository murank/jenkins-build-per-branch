package com.assembla.jenkins

import groovyx.net.http.RESTClient
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.client.HttpResponseException
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

class Api {
    String assemblaServerUrl
    String user
    String password
    String clientId
    String clientToken
    String spaceId
    String spaceToolId

    protected Integer authStep = 0
    protected String accessToken
    protected RESTClient restClient
    protected HttpRequestInterceptor requestInterceptor

    // TODO, when space tool API is ready use it to get gitUrl
    def spaceTool() {
        get(path:"/")
    }

    public void setAssemblaServerUrl(String assemblaServerUrl) {
        if (!assemblaServerUrl.endsWith("/")) assemblaServerUrl += "/"
        this.assemblaServerUrl = assemblaServerUrl
    }

    List<String> getMergeRequestBranchNames() {
        // API response [[target_symbol:master, status:0, source_symbol_type:1, source_symbol:test, space_tool_id:dicbBK8rqr4ycHeJe7dGu1,
        // commit:null, processed_by_user_id:null, apply_status:null, user_id:bgnP_qA1Gr2QjIaaaHk9wZ, applied_at:null,
        // description:Nice try, target_space_tool_id:dicbBK8rqr4ycHeJe7dGu1, updated_at:2012-08-29T05:21:19-07:00, id:155,
        // created_at:2012-08-29T05:21:19-07:00]]

        def response = get(path: "api/v1/spaces/$spaceId/space_tools/$spaceToolId/merge_requests.json?status=open")
        println "Found ${response.data.size()} MRs"

        return response.data.collect {
            println "  ${it.source_symbol}"
            it.source_symbol
        }
    }

    protected get(Map map) {
        // force json api
        map.put("contentType", ContentType.JSON)
        map.put("headers", [Accept: 'application/json'])

        // get is destructive to the map, if there's an error we want the values around still
        Map mapCopy = map.clone() as Map
        def response

        assert mapCopy.path != null, "'path' is a required attribute for the GET method"

        try {
            initRestClient()
            response = this.restClient.get(map)
        } catch(HttpHostConnectException ex) {
            println "Unable to connect to host: $assemblaServerUrl"
            throw ex
        } catch(UnknownHostException ex) {
            println "Unknown host: $assemblaServerUrl"
            throw ex
        } catch(HttpResponseException ex) {
            def message = "Unexpected failure with path $assemblaServerUrl${mapCopy.path}, HTTP Status Code: ${ex.response?.status}, full map: $mapCopy"
            throw new Exception(message, ex)
        }

        assert response.status < 400
        return response
    }

    protected post(String path, postBody = [:], params = [:], ContentType contentType = ContentType.URLENC) {
        HTTPBuilder http = new HTTPBuilder(assemblaServerUrl)

        if (requestInterceptor == null)
            initRestClient()

        http.client.addRequestInterceptor(this.requestInterceptor)

        http.handler.failure = { resp ->
            def msg =  "Unexpected failure on $assemblaServerUrl$path: ${resp.statusLine} ${resp.status}"
            throw new Exception(msg)
        }

        http.request(Method.POST, ContentType.JSON) { req ->
            uri.path = path
            uri.query = params

            if (postBody)
                body = postBody

            response.success = { resp, json ->
                //println(resp.status)
                //println(json)
                return json
            }
        }
    }

    protected void initRestClient() {
        if (restClient == null) {
            this.requestInterceptor = new HttpRequestInterceptor() {
                void process(HttpRequest httpRequest, HttpContext httpContext) {
                    if (authStep == 0) {
                        def auth = clientId + ':' + clientToken
                        httpRequest.addHeader('Authorization', 'Basic ' + auth.bytes.encodeBase64().toString())
                        httpRequest.addHeader('X-Auth-Username', user)
                        httpRequest.addHeader('X-Auth-Password', password)
                    }
                    else {
                        httpRequest.addHeader('Authorization', 'Bearer ' + accessToken)
                    }
                }
            }

            // TODO LATER allow public applications with grant_credentials
            def json = post('api/token', [:], [grant_type: 'password'])
            //println(json.access_token)
            this.accessToken = json.access_token
            authStep = 1

            this.restClient = new RESTClient(assemblaServerUrl)
            restClient.client.addRequestInterceptor(this.requestInterceptor)
        }
    }
}
