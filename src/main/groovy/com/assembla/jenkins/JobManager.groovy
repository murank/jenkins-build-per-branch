package com.assembla.jenkins

import com.entagen.jenkins.JenkinsJobManager
import com.entagen.jenkins.TemplateJob
import com.entagen.jenkins.GitApi
import com.entagen.jenkins.ConcreteJob

class JobManager extends JenkinsJobManager {

    String assemblaUrl
    String apiKey
    String apiSecret
    String spaceId
    String spaceToolId
    String skipUpstream

    private Api assemblaApi

    JobManager(Map props) {
        super(props)
        initApi()
    }

    void syncWithMergeRequests() {
        List<List<String>> allMergeRequests = assemblaApi.mergeRequestInfo
        List<String>       allJobNames      = jenkinsApi.jobNames

        if (skipUpstream) {
            allMergeRequests = skipUpstreamMergeRequests(allMergeRequests)
        }

        // ensure that there is at least one job matching the template pattern, collect the set of template jobs
        List<TemplateJob>  templateJobs     = findRequiredTemplateJobs(allJobNames)

        // create any missing template jobs and delete any jobs matching the template patterns that no longer have merge requests
        syncJobs(allMergeRequests, allJobNames, templateJobs)

        // create any missing branch views, scoped within a nested view if we were given one
        if (!noViews) {
            syncViews(allMergeRequests.collect { it.jobName })
        }
    }

    public List<List<String>> skipUpstreamMergeRequests(List<List<String>> allMergeRequests) {
        allMergeRequests.findAll { !(it.sourceSymbol == "master" && it.targetSymbol == "master") }
    }

    public void syncJobs(List<List<String>> allMergeRequests, List<String> allJobNames, List<TemplateJob> templateJobs) {
        List<String> currentTemplateDrivenJobNames = templateDrivenJobNames(templateJobs, allJobNames)
        List<ConcreteJob> expectedJobs = this.expectedJobs(templateJobs, allMergeRequests)

        createMissingJobs(expectedJobs, currentTemplateDrivenJobNames, templateJobs)
        if (!noDelete) {
            deleteDeprecatedJobs(currentTemplateDrivenJobNames - expectedJobs.jobName)
        }
    }

    public List<ConcreteJob> expectedJobs(List<TemplateJob> templateJobs, List<List<String>> allMergeRequests) {
        allMergeRequests.collect {
            templateJobs.collect {
                TemplateJob templateJob -> templateJob.concreteJobForBranch(it.sourceSymbol, it.jobName)
            }
        }.flatten()
    }

    void initGitApi() {
        initApi()
        super
    }

    Api initApi() {
        if (!assemblaApi) {
            assert apiKey != null
            assert apiSecret != null
            this.assemblaApi = new Api(assemblaServerUrl: assemblaUrl,
                    apiKey: apiKey, apiSecret: apiSecret,
                    spaceId: spaceId, spaceToolId: spaceToolId
            )
            this.gitUrl = assemblaApi.gitUrl
        }

        return this.assemblaApi
    }
}
