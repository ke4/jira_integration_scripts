/**
 * See README.md for copyright details
 */
package actions

import exceptions.TransferException
import models.*
import spock.lang.Specification

/**
 * A test class for stamp transfer action.
 *
 * @author rf9
 *
 */

class StampActionTest extends Specification {

    def "can't stamp between layouts"() {
        setup:
        def sourceLabware = new Labware(labwareType: new LabwareType(layout: new Layout(name: 'test1')))
        def destLabware = new Labware(labwareType: new LabwareType(layout: new Layout(name: 'test2')))

        when:
        TransferActions.stamp(sourceLabware, destLabware, new MaterialType(name: 'test_type'))

        then:
        thrown TransferException
    }

    def "the destination locations should be empty on the destination labware"() {
        setup:
        def labwareType = new LabwareType(name: 'test_type', layout: new Layout(name: 'two plate layout'))
        def locations = [new Location(name: 'A1'), new Location(name: 'A2'), new Location(name: 'A3')]
        def sourceMaterials = [new Material(id: '123'), new Material(id: '456'),  new Material(id: '789')]
        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: labwareType,
            receptacles: [
                new Receptacle(materialUuid: '123', location: locations[0]),
                new Receptacle(materialUuid: '456', location: locations[1]),
                new Receptacle(materialUuid: '789', location: locations[2])
        ])
        def destinationLabware = new Labware(labwareType: labwareType,
            receptacles: [
                new Receptacle(location: locations[0], materialUuid: '9123'),
                new Receptacle(location: locations[1]),
                new Receptacle(location: locations[2], materialUuid: '9124')
            ], barcode: 'TEST_001')

        when:
        destinationLabware = TransferActions.stamp(sourceLabware, destinationLabware, materialType)

        then:
        TransferException ex = thrown()
        ex.message == "The following locations already occupied in the destination labware: A1, A3"
    }

    def "stamping between two plates"() {
        setup:
        def labwareType = new LabwareType(name: 'test_type', layout: new Layout(name: 'two plate layout'))
        def locations = [new Location(name: 'A1'), new Location(name: 'A2')]
        def sourceMaterials = [new Material(id: '123'), new Material(id: '456')]
        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: labwareType, receptacles: [new Receptacle(materialUuid: '123', location: locations[0]), new Receptacle(materialUuid: '456', location: locations[1])])
        def destinationLabware = new Labware(labwareType: labwareType, receptacles: [new Receptacle(location: locations[0]), new Receptacle(location: locations[1])], barcode: 'TEST_001')

        def ids = ['789', '012']
        def newMaterials
        GroovyMock(MaterialActions, global: true)
        GroovyMock(LabwareActions, global: true)

        when:
        destinationLabware = TransferActions.stamp(sourceLabware, destinationLabware, materialType)

        then:
        1 * MaterialActions.getMaterials(sourceMaterials*.id) >> sourceMaterials
        1 * MaterialActions.postMaterials(_) >> { materials ->
            newMaterials = materials[0].eachWithIndex { material, i ->
                material.id = ids[i]
            }
        }
        1 * LabwareActions.updateLabware(destinationLabware) >> destinationLabware

        destinationLabware.receptacles[0].materialUuid == '789'
        destinationLabware.receptacles[1].materialUuid == '012'
        newMaterials.size() == 2
        newMaterials[0].parents[0] == sourceMaterials[0]
        newMaterials[0].name == 'TEST_001_A1'
        newMaterials[1].parents[0] == sourceMaterials[1]
        newMaterials[1].name == 'TEST_001_A2'
    }

    def "stamping with metadata"() {
        setup:
        def labwareType = new LabwareType(name: 'test_type', layout: new Layout(name: 'two plate layout'))
        def locations = [new Location(name: 'A1'), new Location(name: 'A2')]
        def sourceMaterials = [
            new Material(id: '123', metadata: [new Metadatum(key: "key1", value: "value1_1"), new Metadatum(key: "key2", value: "value2_1"), new Metadatum(key: "key3", value: "value3_1")]),
            new Material(id: '456', metadata: [new Metadatum(key: "key1", value: "value1_2"), new Metadatum(key: "key2", value: "value2_2"), new Metadatum(key: "key3", value: "value3_2")])
        ]
        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: labwareType, receptacles: [new Receptacle(materialUuid: '123', location: locations[0]), new Receptacle(materialUuid: '456', location: locations[1])])
        def destinationLabware = new Labware(labwareType: labwareType, receptacles: [new Receptacle(location: locations[0]), new Receptacle(location: locations[1])], barcode: 'TEST_001')

        def ids = ['789', '012']
        def newMaterials
        GroovyMock(MaterialActions, global: true)
        GroovyMock(LabwareActions, global: true)

        when:
        destinationLabware = TransferActions.stamp(sourceLabware, destinationLabware, materialType, ["key1", "key3"])

        then:
        1 * LabwareActions.updateLabware(destinationLabware) >> destinationLabware
        1 * MaterialActions.getMaterials(sourceMaterials*.id) >> sourceMaterials
        1 * MaterialActions.postMaterials(_) >> { materials ->
            newMaterials = materials[0].eachWithIndex { material, i ->
                material.id = ids[i]
            }
        }

        destinationLabware.receptacles[0].materialUuid == '789'
        destinationLabware.receptacles[1].materialUuid == '012'
        newMaterials.size() == 2
        newMaterials[0].parents[0] == sourceMaterials[0]
        newMaterials[0].name == 'TEST_001_A1'
        newMaterials[1].parents[0] == sourceMaterials[1]
        newMaterials[1].name == 'TEST_001_A2'

        newMaterials[0].metadata.size() == 2
        newMaterials[0].metadata[0].key == 'key1'
        newMaterials[0].metadata[0].value == 'value1_1'
        newMaterials[0].metadata[1].key == 'key3'
        newMaterials[0].metadata[1].value == 'value3_1'
        newMaterials[1].metadata.size() == 2
        newMaterials[1].metadata[0].key == 'key1'
        newMaterials[1].metadata[0].value == 'value1_2'
        newMaterials[1].metadata[1].key == 'key3'
        newMaterials[1].metadata[1].value == 'value3_2'
    }

    def "stamping with additional metadata"() {
        setup:
        def labwareType = new LabwareType(name: 'test_type', layout: new Layout(name: 'two plate layout'))
        def locations = [new Location(name: 'A1'), new Location(name: 'A2')]
        def sourceMaterials = [
            new Material(id: '123', metadata: [new Metadatum(key: "key1", value: "value1_1"), new Metadatum(key: "key2", value: "value2_1"), new Metadatum(key: "key3", value: "value3_1")]),
            new Material(id: '456', metadata: [new Metadatum(key: "key1", value: "value1_2"), new Metadatum(key: "key2", value: "value2_2"), new Metadatum(key: "key3", value: "value3_2")])
        ]
        def newMetadata = [new Metadatum(key: 'new_key1', value: "new_value1"), new Metadatum(key: 'new_key2', value: "new_value2")]
        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: labwareType, receptacles: [new Receptacle(materialUuid: '123', location: locations[0]), new Receptacle(materialUuid: '456', location: locations[1])])
        def destinationLabware = new Labware(labwareType: labwareType, receptacles: [new Receptacle(location: locations[0]), new Receptacle(location: locations[1])], barcode: 'TEST_001')

        def ids = ['789', '012']
        def newMaterials
        GroovyMock(MaterialActions, global: true)
        GroovyMock(LabwareActions, global: true)

        when:
        destinationLabware = TransferActions.stamp(sourceLabware, destinationLabware,
            materialType, ["key1", "key3"], newMetadata)

        then:
        1 * LabwareActions.updateLabware(destinationLabware) >> destinationLabware
        1 * MaterialActions.getMaterials(sourceMaterials*.id) >> sourceMaterials
        1 * MaterialActions.postMaterials(_) >> { materials ->
            newMaterials = materials[0].eachWithIndex { material, i ->
                material.id = ids[i]
            }
        }

        destinationLabware.receptacles[0].materialUuid == '789'
        destinationLabware.receptacles[1].materialUuid == '012'
        newMaterials.size() == 2
        newMaterials[0].parents[0] == sourceMaterials[0]
        newMaterials[0].name == 'TEST_001_A1'
        newMaterials[1].parents[0] == sourceMaterials[1]
        newMaterials[1].name == 'TEST_001_A2'

        newMaterials[0].metadata.size() == 4
        newMaterials[0].metadata[0].key == 'key1'
        newMaterials[0].metadata[0].value == 'value1_1'
        newMaterials[0].metadata[1].key == 'key3'
        newMaterials[0].metadata[1].value == 'value3_1'
        newMaterials[0].metadata[2].key == 'new_key1'
        newMaterials[0].metadata[2].value == 'new_value1'
        newMaterials[0].metadata[3].key == 'new_key2'
        newMaterials[0].metadata[3].value == 'new_value2'
        newMaterials[1].metadata.size() == 4
        newMaterials[1].metadata[0].key == 'key1'
        newMaterials[1].metadata[0].value == 'value1_2'
        newMaterials[1].metadata[1].key == 'key3'
        newMaterials[1].metadata[1].value == 'value3_2'
        newMaterials[1].metadata[2].key == 'new_key1'
        newMaterials[1].metadata[2].value == 'new_value1'
        newMaterials[1].metadata[3].key == 'new_key2'
        newMaterials[1].metadata[3].value == 'new_value2'
    }
}