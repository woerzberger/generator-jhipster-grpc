package <%= packageName %>.grpc.entity.<%=entityUnderscoredName%>;

import <%= packageName %>.domain.<%= entityClass %>;
<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) { _%>
import <%= packageName %>.domain.enumeration.<%=fields[idx].fieldType%>;
<%_ }}_%>
import <%= packageName %>.grpc.ProtobufMappers;<% if (dto === 'mapstruct') { %>
import <%= packageName %>.service.dto.<%= instanceType %>;<% } %>
<%
  if (dto !== 'mapstruct') {
    var existingMapperImport = [];
    for (r of relationships) {
      if ((r.relationshipType == 'many-to-many' && r.ownerSide == true)|| r.relationshipType == 'many-to-one' ||(r.relationshipType == 'one-to-one' && r.ownerSide == true)){
        // if the entity is mapped twice, we should implement the mapping once
        if (existingMapperImport.indexOf(r.otherEntityProtoMapper) == -1 && r.otherEntityNameCapitalized !== entityClass) {
          existingMapperImport.push(r.otherEntityProtoMapper);
      %>import <%= r.otherEntityProtoMapper %>;
<% } } } } %>

import org.mapstruct.*;<% if (databaseType === 'cassandra') { %>

import java.util.UUID;<% } %>

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, uses = {ProtobufMappers.class<%
  if (dto !== 'mapstruct') {
    var existingMappings = [];
    for (r of relationships) {
      if ((r.relationshipType == 'many-to-many' && r.ownerSide == true)|| r.relationshipType == 'many-to-one' ||(r.relationshipType == 'one-to-one' && r.ownerSide == true)){
        // if the entity is mapped twice, we should implement the mapping once
        if (existingMappings.indexOf(r.otherEntityNameCapitalized) == -1 && r.otherEntityNameCapitalized !== entityClass) {
          existingMappings.push(r.otherEntityNameCapitalized);
      %>, <%= r.otherEntityNameCapitalized %>ProtoMapper.class<% } } } } %>})
public interface <%= entityClass %>ProtoMapper {

<%_
// Proto -> entity mapping
if (dto  !== 'mapstruct') {
    for (idx in relationships) {
        const relationshipType = relationships[idx].relationshipType;
        const relationshipName = relationships[idx].relationshipName;
        const relationshipNamePlural = relationships[idx].relationshipNamePlural;
        const ownerSide = relationships[idx].ownerSide;
        if (relationshipType == 'many-to-one' || (relationshipType == 'one-to-one' && ownerSide == true)) { _%>
    @Mapping(source = "<%= relationshipName %>Id", target = "<%= relationshipName %>")
    <%_ } else if (relationshipType == 'many-to-many' && ownerSide == false) { _%>
    @Mapping(target = "<%= relationshipNamePlural %>", ignore = true)
    <%_ } else if (relationshipType == 'one-to-many') { _%>
    @Mapping(target = "<%= relationshipNamePlural %>", ignore = true)
    <%_ } else if (relationshipType == 'one-to-one' && ownerSide == false) { _%>
    @Mapping(target = "<%= relationshipName %>", ignore = true)
<%_ } } } _%>
    <%= instanceType %> <%=entityInstance%>ProtoTo<%= instanceType %>(<%= entityClass %>Proto <%=entityInstance%>Proto);

    @AfterMapping
    // Set back null fields : necessary until https://github.com/google/protobuf/issues/2984 is fixed
    default void <%=entityInstance%>ProtoTo<%= instanceType %>(<%= entityClass %>Proto <%=entityInstance%>Proto, @MappingTarget <%= instanceType %> <%= instanceName %>) {
        if ( <%=entityInstance%>Proto == null ) {
            return;
        }

        if(<%=entityInstance%>Proto.getIdOneofCase() != <%= entityClass %>Proto.IdOneofCase.ID) {
            <%= instanceName %>.setId(null);
        }
<%_ for (f of fields) {
    let nullable = false;
    if (!f.isProtobufCustomType && !(f.fieldValidate && f.fieldValidateRules.indexOf('required') != -1)) {
        nullable = true;
    }_%>
    <%_ if (nullable) { _%>
        if(<%= entityInstance %>Proto.get<%= f.fieldInJavaBeanMethod %>OneofCase() != <%= entityClass %>Proto.<%= f.fieldInJavaBeanMethod %>OneofCase.<%= f.fieldNameUnderscored.toUpperCase() %>) {
            <%= instanceName %>.set<%= f.fieldInJavaBeanMethod %>(null);
        }
        <%_ if ((f.fieldDomainType === 'byte[]' || f.fieldDomainType === 'ByteBuffer') && f.fieldTypeBlobContent != 'text') { _%>
        if(<%= entityInstance %>Proto.get<%= f.fieldInJavaBeanMethod %>ContentTypeOneofCase() != <%= entityClass %>Proto.<%= f.fieldInJavaBeanMethod %>ContentTypeOneofCase.<%= f.fieldNameUnderscored.toUpperCase() %>_CONTENT_TYPE) {
            <%= instanceName %>.set<%= f.fieldInJavaBeanMethod %>ContentType(null);
        }
        <%_ } _%>
    <%_ } _%>
<%_ } _%>
<%_for (r of relationships) { _%>
    <%_ if ((r.relationshipType == 'many-to-one' || (r.relationshipType == 'one-to-one' && r.ownerSide == true)) && r.relationshipValidate !== true) { _%>
        if(<%= entityInstance %>Proto.get<%= r.relationshipNameCapitalized %>IdOneofCase() != <%= entityClass %>Proto.<%= r.relationshipNameCapitalized %>IdOneofCase.<%= r.relationshipNameUnderscored.toUpperCase() %>_ID) {
            <%= instanceName %>.set<%= r.relationshipNameCapitalized %><% if (dto === 'mapstruct') { %>Id<% } %>(null);
        }
    <%_ } _%>
<%_ } _%>
    }

    default <%= entityClass %>Proto.Builder create<%= entityClass %>Proto () {
        return <%= entityClass %>Proto.newBuilder();
    }

<%_
// entity -> Proto mapping
if (dto  !== 'mapstruct') {
    for (idx in relationships) {
        const relationshipType = relationships[idx].relationshipType;
        const relationshipName = relationships[idx].relationshipName;
        const ownerSide = relationships[idx].ownerSide;
        if (relationshipType == 'many-to-one' || (relationshipType == 'one-to-one' && ownerSide == true)) {
        _%>
    @Mapping(source = "<%= relationshipName %>.id", target = "<%= relationships[idx].relationshipFieldName %>Id")
    <%_ // Remove since relationship is currently fetched lazily by Hibernate
        if (false) {//if (relationships[idx].otherEntityFieldCapitalized !='Id' && relationships[idx].otherEntityFieldCapitalized != '') { _%>
    @Mapping(source = "<%= relationshipName %>.<%= relationships[idx].otherEntityField %>", target = "<%= relationships[idx].relationshipFieldName %><%= relationships[idx].otherEntityFieldCapitalized %>")
    <%_ } _%>
<%_ } } } _%>
    <%= entityClass %>Proto.Builder <%= instanceName %>To<%= entityClass %>ProtoBuilder(<%= instanceType %> <%= instanceName %>);

    default <%= entityClass %>Proto <%= instanceName %>To<%= entityClass %>Proto(<%= instanceType %> <%= instanceName %>) {
        if (<%= instanceName %> == null) {
            return null;
        }
        return <%= instanceName %>To<%= entityClass %>ProtoBuilder(<%= instanceName %>).build();
    }

<%_ for (field of fields) {
    if(field.fieldIsEnum) {
        var fieldType = field.fieldType;
        let enumValues = field.fieldValues.split(','); _%>
    <%_ for (let idx in enumValues) { _%>
    @ValueMapping(source = "<%= enumValues[idx].trim() %>", target = "<%= field.fieldTypeUpperUnderscored %>_<%= enumValues[idx].trim() %>")
    <%_ } _%>
    <%= entityClass %>Proto.<%= fieldType %>Proto convert<%= fieldType %>To<%= fieldType %>Proto(<%= fieldType %> enumValue);

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    <%_ for (let idx in enumValues) { _%>
    @ValueMapping(source = "<%= field.fieldTypeUpperUnderscored %>_<%= enumValues[idx].trim() %>", target = "<%= enumValues[idx].trim() %>")
    <%_ } _%>
    <%= fieldType %> convert<%= fieldType %>ProtoTo<%= fieldType %>(<%= entityClass %>Proto.<%= fieldType %>Proto enumValue);

<%_ }} _%>
<%_ if(databaseType === 'sql') { _%>
    default <%= entityClass %> <%= entityInstance %>FromId(Long id) {
        if (id == null) {
            return null;
        }
        <%= entityClass %> <%= entityInstance %> = new <%= entityClass %>();
        <%= entityInstance %>.setId(id);
        return <%= entityInstance %>;
    }

<%_ } _%>
}
