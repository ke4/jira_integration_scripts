/**
 * See README.md for copyright details
 */
package uk.ac.sanger.scgcf.jira.services.actions

import spock.lang.Specification
import uk.ac.sanger.scgcf.jira.services.exceptions.TransferException
import uk.ac.sanger.scgcf.jira.services.models.*

/**
 * A test class for split transfer action.
 *
 * @author ke4
 *
 */

class SplitActionTest extends Specification {

    def "destination labware should contains the locations"() {
        setup:
        def sourceLabware = new Labware(labwareType: new LabwareType(layout: new Layout(name: 'test1')), receptacles: [new Receptacle(location: new Location(name: 'A1'))], barcode: 'TEST_001')
        def destinationLocation = ['A1', 'A2']
        def targetLabwareType = new LabwareType(name: 'test_type_plate', layout: new Layout(name: 'test layout with 4 wells'))
        def destinationLabware = new Labware(labwareType: targetLabwareType,
            receptacles: [
                new Receptacle(location: new Location(name: 'A3')),
                new Receptacle(location: new Location(name: 'A4'))],
            barcode: 'TEST_002')

        when:
        TransferActions.split(sourceLabware, destinationLabware,
            new MaterialType(name: 'test_type'), destinationLocation)

        then:
        TransferException ex = thrown()
        ex.message == "The destinations labwares do not have these locations: TEST_002 A1, TEST_002 A2"
    }

    def "the destination locations should be empty on the destination labware"() {
        setup:
        def sourceLabware = new Labware(labwareType: new LabwareType(layout: new Layout(name: 'test1')),
            receptacles: [
                new Receptacle(location: new Location(name: 'A1'), materialUuid: '112233')
            ],
            barcode: 'TEST_001')
        def destinationLocation = ['A1', 'A2']
        def targetLabwareType = new LabwareType(name: 'test_type_plate', layout: new Layout(name: 'test layout with 4 wells'))
        def destinationLabware = new Labware(labwareType: targetLabwareType,
            receptacles: [
                new Receptacle(location: new Location(name: 'A1'), materialUuid: '12345678'),
                new Receptacle(location: new Location(name: 'A2'), materialUuid: '87654321'),
                new Receptacle(location: new Location(name: 'A3'))],
            barcode: 'TEST_002')

        when:
        TransferActions.split(sourceLabware, destinationLabware,
            new MaterialType(name: 'test_type'), destinationLocation)

        then:
        TransferException ex = thrown()
        ex.message == "The following locations already occupied in the destination labware: A1, A2"
    }

    def "split materials into a plate's given location(s)"() {
        setup:
        def sourceLabwareType = new LabwareType(name: 'test_type_tube', layout: new Layout(name: 'test layout for a tube'))
        def targetLabwareType = new LabwareType(name: 'test_type_plate', layout: new Layout(name: 'test layout with 4 wells'))
        def sourceLocation = new Location(name: 'A1')
        def destinationLocations = ['A1', 'A2', 'A3', 'A4']
        def sourceMaterial = new Material(id: '123')
        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: sourceLabwareType, barcode: 'TEST_001',
            receptacles: [new Receptacle(materialUuid: '123', location: sourceLocation)])
        def destinationLabware = Spy(Labware, constructorArgs: [[labwareType: targetLabwareType, barcode: 'TEST_002',
            receptacles: [new Receptacle(location: new Location(name: destinationLocations[0])),
                new Receptacle(location: new Location(name: destinationLocations[1])),
                new Receptacle(location: new Location(name: destinationLocations[2])),
                new Receptacle(location: new Location(name: destinationLocations[3]))]
        ]])

        def ids = ['11', '12', '13', '14']
        def newMaterials = []
        GroovySpy(Material, global: true)

        when:
        destinationLabware = TransferActions.split(sourceLabware, destinationLabware,
            materialType, destinationLocations)

        then:
        1 * Material.getMaterials([sourceMaterial.id]) >> [sourceMaterial]
        1 * Material.postMaterials(_) >> { materials ->
            newMaterials = materials[0].eachWithIndex { material, i ->
                material.id = ids[i]
            }
        }
        1 * destinationLabware.update() >> destinationLabware

        destinationLabware.receptacles[0].materialUuid == '11'
        destinationLabware.receptacles[1].materialUuid == '12'
        destinationLabware.receptacles[2].materialUuid == '13'
        destinationLabware.receptacles[3].materialUuid == '14'
        newMaterials.size() == 4
        newMaterials[0].parents[0] == sourceMaterial
        newMaterials[0].name == 'TEST_002_A1'
        newMaterials[1].parents[0] == sourceMaterial
        newMaterials[1].name == 'TEST_002_A2'
        newMaterials[2].parents[0] == sourceMaterial
        newMaterials[2].name == 'TEST_002_A3'
        newMaterials[3].parents[0] == sourceMaterial
        newMaterials[3].name == 'TEST_002_A4'
    }

    def "split materials  with metadata into a plate's given location(s)"() {
        setup:
        def sourceLabwareType = new LabwareType(name: 'test_type_tube', layout: new Layout(name: 'test layout for a tube'))
        def targetLabwareType = new LabwareType(name: 'test_type_plate', layout: new Layout(name: 'test layout with 4 wells'))
        def sourceLocation = new Location(name: 'A1')
        def destinationLocations = ['A1', 'A2', 'A3', 'A4']
        def sourceMaterial = new Material(id: '123', metadata: [new Metadatum(key: "key1", value: "value1_1"), new Metadatum(key: "key2", value: "value2_1"), new Metadatum(key: "key3", value: "value3_1")])

        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: sourceLabwareType, barcode: 'TEST_001',
            receptacles: [new Receptacle(materialUuid: '123', location: sourceLocation)])
        def destinationLabware = Spy(Labware, constructorArgs: [[labwareType: targetLabwareType, barcode: 'TEST_002',
            receptacles: [new Receptacle(location: new Location(name: destinationLocations[0])),
                new Receptacle(location: new Location(name: destinationLocations[1])),
                new Receptacle(location: new Location(name: destinationLocations[2])),
                new Receptacle(location: new Location(name: destinationLocations[3]))]
        ]])

        def ids = ['11', '12', '13', '14']
        def newMaterials = []
        GroovySpy(Material, global: true)

        when:
        destinationLabware = TransferActions.split(sourceLabware, destinationLabware,
            materialType, destinationLocations, ["key1", "key3"])

        then:
        1 * destinationLabware.update() >> destinationLabware
        1 * Material.getMaterials([sourceMaterial.id]) >> [sourceMaterial]
        1 * Material.postMaterials(_) >> { materials ->
            newMaterials = materials[0].eachWithIndex { material, i ->
                material.id = ids[i]
            }
        }

        destinationLabware.receptacles[0].materialUuid == '11'
        destinationLabware.receptacles[1].materialUuid == '12'
        destinationLabware.receptacles[2].materialUuid == '13'
        destinationLabware.receptacles[3].materialUuid == '14'
        newMaterials.size() == 4
        newMaterials[0].parents[0] == sourceMaterial
        newMaterials[0].name == 'TEST_002_A1'
        newMaterials[1].parents[0] == sourceMaterial
        newMaterials[1].name == 'TEST_002_A2'
        newMaterials[2].parents[0] == sourceMaterial
        newMaterials[2].name == 'TEST_002_A3'
        newMaterials[3].parents[0] == sourceMaterial
        newMaterials[3].name == 'TEST_002_A4'

        newMaterials[0].metadata.size() == 2
        newMaterials[0].metadata[0].key == 'key1'
        newMaterials[0].metadata[0].value == 'value1_1'
        newMaterials[0].metadata[1].key == 'key3'
        newMaterials[0].metadata[1].value == 'value3_1'
        newMaterials[1].metadata.size() == 2
        newMaterials[1].metadata[0].key == 'key1'
        newMaterials[1].metadata[0].value == 'value1_1'
        newMaterials[1].metadata[1].key == 'key3'
        newMaterials[1].metadata[1].value == 'value3_1'
        newMaterials[2].metadata.size() == 2
        newMaterials[2].metadata[0].key == 'key1'
        newMaterials[2].metadata[0].value == 'value1_1'
        newMaterials[2].metadata[1].key == 'key3'
        newMaterials[2].metadata[1].value == 'value3_1'
        newMaterials[3].metadata.size() == 2
        newMaterials[3].metadata[0].key == 'key1'
        newMaterials[3].metadata[0].value == 'value1_1'
        newMaterials[3].metadata[1].key == 'key3'
        newMaterials[3].metadata[1].value == 'value3_1'
    }

    def "split materials with additional metadata into a plate's given location(s)"() {
        setup:
        def sourceLabwareType = new LabwareType(name: 'test_type_tube', layout: new Layout(name: 'test layout for a tube'))
        def targetLabwareType = new LabwareType(name: 'test_type_plate', layout: new Layout(name: 'test layout with 4 wells'))
        def sourceLocation = new Location(name: 'A1')
        def destinationLocations = ['A1', 'A2', 'A3', 'A4']
        def sourceMaterial = new Material(id: '123', metadata: [new Metadatum(key: "key1", value: "value1_1"), new Metadatum(key: "key2", value: "value2_1"), new Metadatum(key: "key3", value: "value3_1")])
        def newMetadata = [
            'A1': [
                new Metadatum(key: 'new_key11', value: "new_value11"),
                new Metadatum(key: 'new_key21', value: "new_value21")
            ],
            'A3': [
                new Metadatum(key: 'new_key13', value: "new_value13"),
                new Metadatum(key: 'new_key23', value: "new_value23")
            ]
        ]
        def materialType = new MaterialType(name: 'new type')

        def sourceLabware = new Labware(labwareType: sourceLabwareType, barcode: 'TEST_001',
            receptacles: [new Receptacle(materialUuid: '123', location: sourceLocation)])
        def destinationLabware = Spy(Labware, constructorArgs: [[labwareType: targetLabwareType,
            receptacles: [new Receptacle(location: new Location(name: destinationLocations[0])),
                new Receptacle(location: new Location(name: destinationLocations[1])),
                new Receptacle(location: new Location(name: destinationLocations[2])),
                new Receptacle(location: new Location(name: destinationLocations[3]))],
            barcode: 'TEST_002'
        ]])

        def ids = ['11', '12', '13', '14']
        def newMaterials = []
        GroovySpy(Material, global: true)

        when:
        destinationLabware = TransferActions.split(sourceLabware, destinationLabware,
            materialType, destinationLocations, ["key1", "key3"], newMetadata)

        then:
        1 * destinationLabware.update() >> destinationLabware
        1 * Material.getMaterials([sourceMaterial.id]) >> [sourceMaterial]
        1 * Material.postMaterials(_) >> { materials ->
            newMaterials = materials[0].eachWithIndex { material, i ->
                material.id = ids[i]
            }
        }

        destinationLabware.receptacles[0].materialUuid == '11'
        destinationLabware.receptacles[1].materialUuid == '12'
        destinationLabware.receptacles[2].materialUuid == '13'
        destinationLabware.receptacles[3].materialUuid == '14'
        newMaterials.size() == 4
        newMaterials[0].parents[0] == sourceMaterial
        newMaterials[0].name == 'TEST_002_A1'
        newMaterials[1].parents[0] == sourceMaterial
        newMaterials[1].name == 'TEST_002_A2'
        newMaterials[2].parents[0] == sourceMaterial
        newMaterials[2].name == 'TEST_002_A3'
        newMaterials[3].parents[0] == sourceMaterial
        newMaterials[3].name == 'TEST_002_A4'

        newMaterials[0].metadata.size() == 4
        newMaterials[0].metadata[0].key == 'key1'
        newMaterials[0].metadata[0].value == 'value1_1'
        newMaterials[0].metadata[1].key == 'key3'
        newMaterials[0].metadata[1].value == 'value3_1'
        newMaterials[0].metadata[2].key == 'new_key11'
        newMaterials[0].metadata[2].value == 'new_value11'
        newMaterials[0].metadata[3].key == 'new_key21'
        newMaterials[0].metadata[3].value == 'new_value21'
        newMaterials[1].metadata.size() == 2
        newMaterials[1].metadata[0].key == 'key1'
        newMaterials[1].metadata[0].value == 'value1_1'
        newMaterials[1].metadata[1].key == 'key3'
        newMaterials[1].metadata[1].value == 'value3_1'
        newMaterials[2].metadata.size() == 4
        newMaterials[2].metadata[0].key == 'key1'
        newMaterials[2].metadata[0].value == 'value1_1'
        newMaterials[2].metadata[1].key == 'key3'
        newMaterials[2].metadata[1].value == 'value3_1'
        newMaterials[2].metadata[2].key == 'new_key13'
        newMaterials[2].metadata[2].value == 'new_value13'
        newMaterials[2].metadata[3].key == 'new_key23'
        newMaterials[2].metadata[3].value == 'new_value23'
        newMaterials[3].metadata.size() == 2
        newMaterials[3].metadata[0].key == 'key1'
        newMaterials[3].metadata[0].value == 'value1_1'
        newMaterials[3].metadata[1].key == 'key3'
        newMaterials[3].metadata[1].value == 'value3_1'
    }
}
