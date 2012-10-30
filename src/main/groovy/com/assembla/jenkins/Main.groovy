package com.assembla.jenkins

import com.entagen.jenkins.CliTools

/*
Bootstrap class that parses command line arguments, or system properties passed in by jenkins, and starts the jenkins-build-per-branch sync process
*/
class Main extends CliTools {

    public static final Map<String, Map<String, Object>> cliOpts = [
            h: [longOpt: 'help', required: false, args: 0, argName: 'help', description: "Print usage information - gradle flag -Dhelp=true"],
            j: [longOpt: 'jenkins-url', required: true, args: 1, argName: 'jenkinsUrl', description: "Jenkins URL - gradle flag -DjenkinsUrl=<jenkinsUrl>"],
            p: [longOpt: 'job-prefix', required: true, args: 1, argName: 'templateJobPrefix', description: "Template Job Prefix, - gradle flag -DtemplateJobPrefix=<jobPrefix>"],
            t: [longOpt: 'template-branch', required: true, args: 1, argName: 'templateBranchName', description: "Template Branch Name - gradle flag -DtemplateBranchName=<branchName>"],
            n: [longOpt: 'nested-view', required: false, args: 1, argName: 'nestedView', description: "Nested Parent View Name - gradle flag -DnestedView=<nestedView> - optional - must have Jenkins Nested View Plugin installed"],
            c: [longOpt: 'print-config', required: false, args: 0, argName: 'printConfig', description: "Check configuration - print out settings then exit - gradle flag -DprintConfig=true"],
            d: [longOpt: 'dry-run', required: false, args: 0, argName: 'dryRun', description: "Dry run, don't actually modify, create, or delete any jobs, just print out what would happen - gradle flag: -DdryRun=true"],
            v: [longOpt: 'no-views', required: false, args: 0, argName: 'noViews', description: "Suppress view creation - gradle flag -DnoViews=true"],
            k: [longOpt: 'no-delete', required: false, args: 0, argName: 'noDelete', description: "Do not delete (keep) branches and views - gradle flag -DnoDelete=true"],
            f: [longOpt: 'filter-branch-names', required: false, args: 1, argName: 'branchNameRegex', description: "Only branches matching the regex will be accepted - gradle flag: -DbranchNameRegex=<regex>"],
            usr: [longOpt: 'jenkins-user', required: false, args: 1, argName: 'jenkinsUser', description: "Jenkins username - gradle flag -DjenkinsUser=<jenkinsUser>"],
            pwd: [longOpt: 'jenkins-password', required: false, args: 1, argName: 'jenkinsPassword', description: "Jenkins password - gradle flag -DjenkinsPassword=<jenkinsPassword>"],

            aurl: [longOpt: 'assembla-url', required: false, args: 1, argName: 'assemblaUrl', description: "Assembla server url - gradle flag -DassemblaUrl=<assemblaUrl>"],
            apk:  [longOpt: 'api-key', required: true, args: 1, argName: 'apiKey', description: "Assembla Api - gradle flag -DapiKey=<apiKey>"],
            aps:  [longOpt: 'api-secret', required: true, args: 1, argName: 'apiSecret', description: "Assembla Api - gradle flag -DapiSecret=<apiSecret>"],
            s:    [longOpt: 'space-id', required: true, args: 1, argName: 'spaceId', description: "Space Id - gradle flag -DspaceId=<spaceId>"],
            s1:    [longOpt: 'space-tool-id', required: true, args: 1, argName: 'spaceToolId', description: "Space tool Id - gradle flag -DspaceToolId=<spaceToolId>"]
    ]

    public static void main(String[] args) {
        Map<String, String> argsMap = parseArgs(args)
        showConfiguration(argsMap)
        JobManager manager = new JobManager(argsMap)
        manager.syncWithMergeRequests()
    }

    public static Map<String, String> parseArgs(String[] args) {
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

        def missingArgs = cliOpts.findAll { shortOpt, optMap ->
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
        def cli = new CliBuilder(usage: "jenkins-build-per-branch [options]", header: 'Options, if calling from `gradle syncWithGit`, you need to use a system property format -D<argName>=value, ex: (gradle -DgitUrl=git@github.com:yourname/yourrepo.git syncWithGit):')
        cliOpts.each { String shortOpt, Map<String, Object> optMap ->
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
        cliOpts.each { String shortOpt, Map<String, String> optMap ->
            if (optMap.argName) {
                mergedArgs[optMap.argName] = commandLineOptions."$shortOpt" ?: System.getProperty(optMap.argName)
            }
        }
        return mergedArgs.findAll { k, v -> v }
    }
}
