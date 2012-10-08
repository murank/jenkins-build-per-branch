package com.assembla.jenkins

import com.entagen.jenkins.JenkinsJobManager
import com.entagen.jenkins.TemplateJob
import com.entagen.jenkins.GitApi

class JobManager extends JenkinsJobManager {

    String assemblaUrl
    String apiKey
    String apiSecret
    String spaceId
    String spaceToolId

    private Api assemblaApi

    JobManager(Map props) {
        super(props)
        initApi()
    }

    void syncWithMergeRequests() {
        List<String> allBranchNames = assemblaApi.mergeRequestBranchNames
        List<String> allJobNames = jenkinsApi.jobNames

        // ensure that there is at least one job matching the template pattern, collect the set of template jobs
        List<TemplateJob> templateJobs = findRequiredTemplateJobs(allJobNames)

        // create any missing template jobs and delete any jobs matching the template patterns that no longer have branches
        syncJobs(allBranchNames, allJobNames, templateJobs)

        // create any missing branch views, scoped within a nested view if we were given one
        if (!noViews) {
            syncViews(allBranchNames)
        }
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
