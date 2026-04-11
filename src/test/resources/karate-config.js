function fn() {
    // karate.port is set by the JUnit5 test runner from @LocalServerPort
    var port = karate.properties['karate.port'] || '8080';
    var baseUrl = 'http://localhost:' + port;

    karate.log('karate.env =', karate.env);
    karate.log('baseUrl =', baseUrl);

    var config = {
        baseUrl: baseUrl
    };

    // Set default headers for all requests
    karate.configure('headers', { 'Content-Type': 'application/json', 'Accept': 'application/json' });

    // Increase read timeout for slower CI environments
    karate.configure('readTimeout', 10000);

    return config;
}
