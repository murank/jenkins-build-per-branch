package com.assembla.jenkins

class Check {
    static String apiUrl = 'https://core.m-stage.assembla.cc/'

    public static void main(String[] args) {
        checkMrAPI()
//        setup()
    }

    private static void checkMrAPI() {
        // https://core.m-stage.assembla.cc/user/edit/manage_clients
        def apiKey = '1713109821a9028160c9'
        def apiSecret = '6539899d5ffb1cfff51e5b33d9f11c8122220984'

        def api = new Api(assemblaServerUrl: apiUrl,
                apiKey: apiKey, apiSecret: apiSecret,
                spaceId: 'groovy32', spaceToolId: 'git')
        //println api.mergeRequestBranchNames
        println api.gitUrl
    }
}
