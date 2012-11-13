package com.entagen.jenkins

class TemplateJob {
    String jobName
    String baseJobName
    String templateBranchName

    String fixJobName(String unsafeName) {
        // git branches often have a forward slash in them, but they make jenkins cranky, turn it into an underscore
        String safeName = unsafeName.replaceAll('/', '_')
        return "$baseJobName-$safeName"
    }

    ConcreteJob concreteJobForBranch(String branchName) {
        ConcreteJob concreteJob = new ConcreteJob(templateJob: this, branchName: branchName, jobName: fixJobName(branchName) )
    }

    ConcreteJob concreteJobForBranch(String branchName, String jobName) {
        ConcreteJob concreteJob = new ConcreteJob(templateJob: this, branchName: branchName, jobName: fixJobName(jobName) )
    }
}
