/**
 * See README.md for copyright details
 */
package uk.ac.sanger.scgcf.jira.services.models

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import org.apache.log4j.Logger

import uk.ac.sanger.scgcf.jira.services.converters.MaterialBatchConverter
import uk.ac.sanger.scgcf.jira.services.utils.RestService
import uk.ac.sanger.scgcf.jira.services.utils.RestServiceConfig

/**
 * The model used to represent the {@code Material} entity used by the JSON API Converter.
 * 
 * @author ke4
 *
 */
@Type("materials")
class Material extends BaseModel {

    @Id
    String id
    String name

    @Relationship("material_type")
    MaterialType materialType

    @Relationship("metadata")
    List<Metadatum> metadata

    @Relationship("parents")
    List<Material> parents

    @Relationship("children")
    List<Material> children

    static RestService restService = RestService.MATERIAL_SERVICE

    static Logger LOG = Logger.getLogger(Material.class);

    /**
     * Get a list of {@code Material} objects from the JSON API by their IDs.
     * @param materialUuids The material IDs to find
     * @return The list of materials
     */
    static List<Material> getMaterials(Collection<Material> materialUuids) {
        LOG.debug("Get Materials with the following ids: ${materialUuids.collect { it }.join(', ')} ")
        postMaterials(materialUuids.collect { new Material(id: it) })
    }

    /**
     * Create a new {@code Material} in the database.
     * @param materialName The name of the new {@code Material}
     * @param materialType The name of the new {@code Material}'s type
     * @param metadata A list of metadata object to be added to the {@code Material}
     * @return The persisted {@code Material}
     */
    static Material create(String materialName, String materialType, Collection<Metadatum> metadata = []) {
        def material = new Material(
            name: materialName,
            materialType: new MaterialType(name: materialType),
            metadata: metadata
        )
        LOG.debug("Creating a material with the following parameters: ${material.toString()}")
        postMaterials([material])[0]
    }

    /**
     * Create or update a collection of {@code Material}s in the database.
     * @param materials The {@code Material}s to be persisted. Is not modified.
     * @return The new {@code Material}s
     */
    static List<Material> postMaterials(Collection<Material> materials) {
        LOG.debug("Post the following Materials: ${materials.collect { it.name }.join(', ')} ")
        def postJson = MaterialBatchConverter.convertObjectToJson(new MaterialBatch(materials: materials))
        LOG.debug("Converted JSON message from Material: $postJson")

        materials.collect {
            if (it.id == null) {
                it.id = UUID.randomUUID().toString()
            }
            if (it.materialType == null) {
                it.materialType = new MaterialType(name: "sample")
            }
            if (it.getParents() == null || it.getParents().size() == 0) {
                Material parentMaterial = new Material(name: "parentMaterialName", materialType: new MaterialType(name: "sample"))
                it.setParents(Arrays.asList(parentMaterial))
            }
            it
        }
    }
}
