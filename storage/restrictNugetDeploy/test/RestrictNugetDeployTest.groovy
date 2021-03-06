import spock.lang.Specification
import org.jfrog.artifactory.client.model.repository.settings.impl.NugetRepositorySettingsImpl
import groovy.util.XmlParser
import groovy.xml.XmlUtil
import org.jfrog.artifactory.client.ArtifactoryClientBuilder

class RestrictNugetDeployTest extends Specification {
    def 'restrict nuget deploy test'() {
        setup:
        def baseurl = 'http://localhost:8088/artifactory'
        def artifactory = ArtifactoryClientBuilder.create().setUrl(baseurl)
            .setUsername('admin').setPassword('password').build()

        def builder = artifactory.repositories().builders()
        def local = builder.localRepositoryBuilder().key('nuget-local')
        .repositorySettings(new NugetRepositorySettingsImpl()).build()
        artifactory.repositories().create(0, local)

        def remote = builder.remoteRepositoryBuilder().key('nuget-remote')
        remote.repositorySettings(new NugetRepositorySettingsImpl())
        remote.url('https://nuget.org')
        artifactory.repositories().create(0, remote.build())
        // TODO when creating a nuget repository via the REST API, the
        // feedContextPath and downloadContextPath properties do not get set,
        // even when they are provided. remove this hack whenever this gets fixed
        def conn = new URL("$baseurl/api/system/configuration").openConnection()
        conn.requestMethod = 'GET'
        conn.setRequestProperty('Authorization', "Basic ${'admin:password'.bytes.encodeBase64()}")
        def xml = new XmlParser().parse(conn.inputStream)
        assert conn.responseCode == 200
        conn.disconnect()
        def nuget = xml.remoteRepositories[0].remoteRepository.find { it.key.text() == 'nuget-remote' }
        def instxt = '<nuget xmlns="' + (xml.name() =~ '^\\{(.*)\\}config$')[0][1]
        instxt += '" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">'
        instxt += '<feedContextPath>api/v2</feedContextPath>'
        instxt += '<downloadContextPath>api/v2/package</downloadContextPath></nuget>'
        def ins = new XmlParser().parseText(instxt)
        nuget.children().add(1 + nuget.findIndexOf { it.name().toString().contains('rejectInvalidJars') }, ins)
        conn = new URL("$baseurl/api/system/configuration").openConnection()
        conn.requestMethod = 'POST'
        conn.doOutput = true
        conn.setRequestProperty('Authorization', "Basic ${'admin:password'.bytes.encodeBase64()}")
        conn.setRequestProperty('Content-Type', 'application/xml')
        conn.outputStream << XmlUtil.serialize(xml)
        assert conn.responseCode == 200
        conn.disconnect()

        def jquery = new File('./src/test/groovy/RestrictNugetDeployTest/jquery.3.1.1.nupkg')
        
        when:
        def resp = artifactory.repository('nuget-local').upload('jQuery Foundation, Inc./jQuery/jQuery.3.1.1.nupkg', jquery).doUpload()
        
        then:
        resp.size == 0

        when:
        resp = artifactory.repository('nuget-local').upload('jQuery Foundation, Inc./new/new.3.1.1.nupkg', jquery).doUpload()
    
        then:
        resp.size > 0

        cleanup:
        artifactory.repository('nuget-local').delete()
        artifactory.repository('nuget-remote').delete()
    }
}
