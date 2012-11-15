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
    String assemblaServerUrl = "https://api.assembla.com/"
    String apiKey
    String apiSecret
    String spaceId
    String spaceToolId

    protected RESTClient restClient
    protected HttpRequestInterceptor requestInterceptor

    public String getPinUrl() {
        assemblaServerUrl + "authorization?client_id=$clientId&response_type=pin_code"
    }

    // TODO, when space tool API is ready use it to get gitUrl
    public String getGitUrl() {
        def response = get(path:"v1/spaces/$spaceId/space_tools/${spaceToolId}.json")
        //println(response.data)
        return response.data.url
    }

    public void setAssemblaServerUrl(String assemblaServerUrl) {
        if (assemblaServerUrl != null) {
          if (!assemblaServerUrl.endsWith("/")) assemblaServerUrl += "/"
          this.assemblaServerUrl = assemblaServerUrl
        }
    }

    List<List<String>> getMergeRequestInfo() {
        // API response [[target_symbol:master, status:0, source_symbol_type:1, source_symbol:test, space_tool_id:dicbBK8rqr4ycHeJe7dGu1,
        // commit:null, processed_by_user_id:null, apply_status:null, user_id:bgnP_qA1Gr2QjIaaaHk9wZ, applied_at:null,
        // description:Nice try, target_space_tool_id:dicbBK8rqr4ycHeJe7dGu1, updated_at:2012-08-29T05:21:19-07:00, id:155,
        // created_at:2012-08-29T05:21:19-07:00]]

        def response = get(path: "v1/spaces/$spaceId/space_tools/$spaceToolId/merge_requests.json", query: [status: "open"])
        def result = []

        if (response.status != 204) {
            println "Found ${response.data.size()} MRs"

            result = response.data.collect {
                def targetSpaceToolId = it.target_space_tool_id
                def targetSpaceId     = it.target_space_id
                def spaceToolResponse = get(path: "v1/spaces/$targetSpaceId/space_tools/${targetSpaceToolId}.json")
                def targetRemote      = spaceToolResponse.data.url

                [
                    id: it.id,
                    title: it.title,
                    sourceSymbol: it.source_symbol,
                    targetSymbol: it.target_symbol,
                    jobName: it.source_symbol + "-" + it.target_symbol + "-" + it.id.toString(),
                    targetRemote: targetRemote
                ]
            }
        }
        else {
            println "There are no MRs..."
        }

        return result
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
                    httpRequest.addHeader('X-Api-Key', apiKey)
                    httpRequest.addHeader('X-Api-Secret', apiSecret)
                }
            }

            this.restClient = new RESTClient(assemblaServerUrl)
            restClient.client.addRequestInterceptor(this.requestInterceptor)
        }
    }
}
