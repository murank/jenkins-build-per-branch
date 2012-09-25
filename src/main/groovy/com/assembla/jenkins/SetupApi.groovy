package com.assembla.jenkins

import com.entagen.jenkins.CliTools
import oauth.signpost.basic.*
import oauth.signpost.OAuth
import groovyx.net.http.RESTClient

class SetupApi extends CliTools {
    static Map<String, Map<String, Object>> options = [
            h: [longOpt: 'help', required: false, args: 0, argName: 'help', description: "Print usage information - gradle flag -Dhelp=true"],
            aurl: [longOpt: 'assembla-url', required: false, args: 1, argName: 'assemblaUrl', description: "Assembla server url - gradle flag -DassemblaUrl=<assemblaUrl>"],
            cid:  [longOpt: 'client-id', required: true, args: 1, argName: 'clientId', description: "OAuth2 Client Id - gradle flag -DclientId=<clientId>"],
    ]

    public static void main(String[] args) {
        Map<String, String> argsMap = parseArgs(args)
        showConfiguration(argsMap)
        def assemblaApi = new Api(clientId: argsMap.clientId)
        setupApi(assemblaApi)
    }

    public static void setupApi(Api assemblaApi) {
        println "Please copy below url and paste it in your browser ${assemblaApi.pinUrl}"
        println "Then login and copy the pin code from the web page and enter it."
        println assemblaApi.pinUrl
        println()

        print "PIN:"
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in))

        def pinCode = '9492505' //br.readLine()
        println "You entered: $pinCode"

        def rest = new RESTClient(assemblaApi.assemblaServerUrl)
        rest.auth.basic assemblaApi.clientId, assemblaApi.clientToken
        println rest.post(path: "token?grant_type=pin_code&pin_code=$pinCode")

        // https://_client_id:_client_secret@api.assembla.com/token?grant_type=pin_code&pin_code=_pin_code


//        def consumer = new DefaultOAuthConsumer(assemblaApi.clientId, assemblaApi.clientToken)
//        def provider = new DefaultOAuthProvider(
//                                 "${assemblaApi.assemblaServerUrl}token",
//                                 "${assemblaApi.assemblaServerUrl}token",
//                                 "${assemblaApi.assemblaServerUrl}authorization");
//        println provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);
//        provider.retrieveAccessToken(consumer, pinCode)
//        println consumer.token
//        println consumer.tokenSecret
    }

    static Map<String, String> parseArgs(String[] args) {
        def cli = createCliBuilder()
        OptionAccessor commandLineOptions = cli.parse(args)

        // this is necessary as Gradle's command line parsing stinks, it only allows you to pass in system properties (or task properties which are basically the same thing)
        // we need to merge in those properties in case the script is being called from `gradle syncWithGit` and the user is giving us system properties
        Map<String, String> argsMap = mergeSystemPropertyOptions(commandLineOptions)

        if (argsMap.help) {
            cli.usage()
            System.exit(0)
        }

        if (argsMap.printConfig) {
            showConfiguration(argsMap)
            System.exit(0)
        }

        def missingArgs = options.findAll { shortOpt, optMap ->
            if (optMap.required) return !argsMap."${optMap.argName}"
        }

        if(missingArgs) {
            missingArgs.each {shortOpt, missingArg -> println "missing required argument: ${missingArg.argName}"}
            cli.usage()
            System.exit(1)
        }

        return argsMap
    }

    public static createCliBuilder() {
        def cli = new CliBuilder(usage: "setupAssemblaApi [options]", header: 'Options, if calling from `gradle setupAssemblaApi`, you need to use a system property format -D<argName>=value, ex: (gradle -DclientId setupAssemblaApi):')
        options.each { String shortOpt, Map<String, Object> optMap ->
            if (optMap.args) {
                cli."$shortOpt"(longOpt: optMap.longOpt, args: optMap.args, argName: optMap.argName, optMap.description)
            } else {
                cli."$shortOpt"(longOpt: optMap.longOpt, optMap.description)
            }
        }
        return cli
    }

    public static Map<String, String> mergeSystemPropertyOptions(OptionAccessor commandLineOptions) {
        Map <String, String> mergedArgs = [:]
        options.each { String shortOpt, Map<String, String> optMap ->
            if (optMap.argName) {
                mergedArgs[optMap.argName] = commandLineOptions."$shortOpt" ?: System.getProperty(optMap.argName)
            }
        }
        return mergedArgs.findAll { k, v -> v }
    }
}
