// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

/**
 * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation}
 */
public final class IrMultiFieldValueClassRepresentation extends
    org.jetbrains.kotlin.protobuf.GeneratedMessageLite implements
    // @@protoc_insertion_point(message_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation)
    IrMultiFieldValueClassRepresentationOrBuilder {
  // Use IrMultiFieldValueClassRepresentation.newBuilder() to construct.
  private IrMultiFieldValueClassRepresentation(org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder builder) {
    super(builder);
    this.unknownFields = builder.getUnknownFields();
  }
  private IrMultiFieldValueClassRepresentation(boolean noInit) { this.unknownFields = org.jetbrains.kotlin.protobuf.ByteString.EMPTY;}

  private static final IrMultiFieldValueClassRepresentation defaultInstance;
  public static IrMultiFieldValueClassRepresentation getDefaultInstance() {
    return defaultInstance;
  }

  public IrMultiFieldValueClassRepresentation getDefaultInstanceForType() {
    return defaultInstance;
  }

  private final org.jetbrains.kotlin.protobuf.ByteString unknownFields;
  private IrMultiFieldValueClassRepresentation(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    initFields();
    int mutable_bitField0_ = 0;
    org.jetbrains.kotlin.protobuf.ByteString.Output unknownFieldsOutput =
        org.jetbrains.kotlin.protobuf.ByteString.newOutput();
    org.jetbrains.kotlin.protobuf.CodedOutputStream unknownFieldsCodedOutput =
        org.jetbrains.kotlin.protobuf.CodedOutputStream.newInstance(
            unknownFieldsOutput, 1);
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(input, unknownFieldsCodedOutput,
                                   extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
              property_ = new java.util.ArrayList<org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property>();
              mutable_bitField0_ |= 0x00000001;
            }
            property_.add(input.readMessage(org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.PARSER, extensionRegistry));
            break;
          }
        }
      }
    } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException(
          e.getMessage()).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
        property_ = java.util.Collections.unmodifiableList(property_);
      }
      try {
        unknownFieldsCodedOutput.flush();
      } catch (java.io.IOException e) {
      // Should not happen
      } finally {
        unknownFields = unknownFieldsOutput.toByteString();
      }
      makeExtensionsImmutable();
    }
  }
  public static org.jetbrains.kotlin.protobuf.Parser<IrMultiFieldValueClassRepresentation> PARSER =
      new org.jetbrains.kotlin.protobuf.AbstractParser<IrMultiFieldValueClassRepresentation>() {
    public IrMultiFieldValueClassRepresentation parsePartialFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return new IrMultiFieldValueClassRepresentation(input, extensionRegistry);
    }
  };

  @java.lang.Override
  public org.jetbrains.kotlin.protobuf.Parser<IrMultiFieldValueClassRepresentation> getParserForType() {
    return PARSER;
  }

  public interface PropertyOrBuilder extends
      // @@protoc_insertion_point(interface_extends:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property)
      org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder {

    /**
     * <code>required int32 name = 1;</code>
     */
    boolean hasName();
    /**
     * <code>required int32 name = 1;</code>
     */
    int getName();

    /**
     * <code>required int32 type = 2;</code>
     */
    boolean hasType();
    /**
     * <code>required int32 type = 2;</code>
     */
    int getType();
  }
  /**
   * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property}
   */
  public static final class Property extends
      org.jetbrains.kotlin.protobuf.GeneratedMessageLite implements
      // @@protoc_insertion_point(message_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property)
      PropertyOrBuilder {
    // Use Property.newBuilder() to construct.
    private Property(org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private Property(boolean noInit) { this.unknownFields = org.jetbrains.kotlin.protobuf.ByteString.EMPTY;}

    private static final Property defaultInstance;
    public static Property getDefaultInstance() {
      return defaultInstance;
    }

    public Property getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final org.jetbrains.kotlin.protobuf.ByteString unknownFields;
    private Property(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      org.jetbrains.kotlin.protobuf.ByteString.Output unknownFieldsOutput =
          org.jetbrains.kotlin.protobuf.ByteString.newOutput();
      org.jetbrains.kotlin.protobuf.CodedOutputStream unknownFieldsCodedOutput =
          org.jetbrains.kotlin.protobuf.CodedOutputStream.newInstance(
              unknownFieldsOutput, 1);
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFieldsCodedOutput,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 8: {
              bitField0_ |= 0x00000001;
              name_ = input.readInt32();
              break;
            }
            case 16: {
              bitField0_ |= 0x00000002;
              type_ = input.readInt32();
              break;
            }
          }
        }
      } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        try {
          unknownFieldsCodedOutput.flush();
        } catch (java.io.IOException e) {
        // Should not happen
        } finally {
          unknownFields = unknownFieldsOutput.toByteString();
        }
        makeExtensionsImmutable();
      }
    }
    public static org.jetbrains.kotlin.protobuf.Parser<Property> PARSER =
        new org.jetbrains.kotlin.protobuf.AbstractParser<Property>() {
      public Property parsePartialFrom(
          org.jetbrains.kotlin.protobuf.CodedInputStream input,
          org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
          throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
        return new Property(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public org.jetbrains.kotlin.protobuf.Parser<Property> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    public static final int NAME_FIELD_NUMBER = 1;
    private int name_;
    /**
     * <code>required int32 name = 1;</code>
     */
    public boolean hasName() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required int32 name = 1;</code>
     */
    public int getName() {
      return name_;
    }

    public static final int TYPE_FIELD_NUMBER = 2;
    private int type_;
    /**
     * <code>required int32 type = 2;</code>
     */
    public boolean hasType() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required int32 type = 2;</code>
     */
    public int getType() {
      return type_;
    }

    private void initFields() {
      name_ = 0;
      type_ = 0;
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      if (!hasName()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasType()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(org.jetbrains.kotlin.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeInt32(1, name_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeInt32(2, type_);
      }
      output.writeRawBytes(unknownFields);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += org.jetbrains.kotlin.protobuf.CodedOutputStream
          .computeInt32Size(1, name_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += org.jetbrains.kotlin.protobuf.CodedOutputStream
          .computeInt32Size(2, type_);
      }
      size += unknownFields.size();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(
        org.jetbrains.kotlin.protobuf.ByteString data)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(
        org.jetbrains.kotlin.protobuf.ByteString data,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(byte[] data)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(
        byte[] data,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(
        java.io.InputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseDelimitedFrom(
        java.io.InputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parseFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    /**
     * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property}
     */
    public static final class Builder extends
        org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder<
          org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property, Builder>
        implements
        // @@protoc_insertion_point(builder_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property)
        org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.PropertyOrBuilder {
      // Construct using org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        name_ = 0;
        bitField0_ = (bitField0_ & ~0x00000001);
        type_ = 0;
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property getDefaultInstanceForType() {
        return org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.getDefaultInstance();
      }

      public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property build() {
        org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property buildPartial() {
        org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property result = new org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.name_ = name_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.type_ = type_;
        result.bitField0_ = to_bitField0_;
        return result;
      }

      public Builder mergeFrom(org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property other) {
        if (other == org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.getDefaultInstance()) return this;
        if (other.hasName()) {
          setName(other.getName());
        }
        if (other.hasType()) {
          setType(other.getType());
        }
        setUnknownFields(
            getUnknownFields().concat(other.unknownFields));
        return this;
      }

      public final boolean isInitialized() {
        if (!hasName()) {
          
          return false;
        }
        if (!hasType()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          org.jetbrains.kotlin.protobuf.CodedInputStream input,
          org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private int name_ ;
      /**
       * <code>required int32 name = 1;</code>
       */
      public boolean hasName() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required int32 name = 1;</code>
       */
      public int getName() {
        return name_;
      }
      /**
       * <code>required int32 name = 1;</code>
       */
      public Builder setName(int value) {
        bitField0_ |= 0x00000001;
        name_ = value;
        
        return this;
      }
      /**
       * <code>required int32 name = 1;</code>
       */
      public Builder clearName() {
        bitField0_ = (bitField0_ & ~0x00000001);
        name_ = 0;
        
        return this;
      }

      private int type_ ;
      /**
       * <code>required int32 type = 2;</code>
       */
      public boolean hasType() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required int32 type = 2;</code>
       */
      public int getType() {
        return type_;
      }
      /**
       * <code>required int32 type = 2;</code>
       */
      public Builder setType(int value) {
        bitField0_ |= 0x00000002;
        type_ = value;
        
        return this;
      }
      /**
       * <code>required int32 type = 2;</code>
       */
      public Builder clearType() {
        bitField0_ = (bitField0_ & ~0x00000002);
        type_ = 0;
        
        return this;
      }

      // @@protoc_insertion_point(builder_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property)
    }

    static {
      defaultInstance = new Property(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property)
  }

  public static final int PROPERTY_FIELD_NUMBER = 1;
  private java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property> property_;
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
   */
  public java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property> getPropertyList() {
    return property_;
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
   */
  public java.util.List<? extends org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.PropertyOrBuilder> 
      getPropertyOrBuilderList() {
    return property_;
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
   */
  public int getPropertyCount() {
    return property_.size();
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property getProperty(int index) {
    return property_.get(index);
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.PropertyOrBuilder getPropertyOrBuilder(
      int index) {
    return property_.get(index);
  }

  private void initFields() {
    property_ = java.util.Collections.emptyList();
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    for (int i = 0; i < getPropertyCount(); i++) {
      if (!getProperty(i).isInitialized()) {
        memoizedIsInitialized = 0;
        return false;
      }
    }
    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(org.jetbrains.kotlin.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    for (int i = 0; i < property_.size(); i++) {
      output.writeMessage(1, property_.get(i));
    }
    output.writeRawBytes(unknownFields);
  }

  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;

    size = 0;
    for (int i = 0; i < property_.size(); i++) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeMessageSize(1, property_.get(i));
    }
    size += unknownFields.size();
    memoizedSerializedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  @java.lang.Override
  protected java.lang.Object writeReplace()
      throws java.io.ObjectStreamException {
    return super.writeReplace();
  }

  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(byte[] data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(
      byte[] data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseDelimitedFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }

  /**
   * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation}
   */
  public static final class Builder extends
      org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder<
        org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation, Builder>
      implements
      // @@protoc_insertion_point(builder_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation)
      org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentationOrBuilder {
    // Construct using org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
    }
    private static Builder create() {
      return new Builder();
    }

    public Builder clear() {
      super.clear();
      property_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation getDefaultInstanceForType() {
      return org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.getDefaultInstance();
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation build() {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation buildPartial() {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation result = new org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation(this);
      int from_bitField0_ = bitField0_;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        property_ = java.util.Collections.unmodifiableList(property_);
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.property_ = property_;
      return result;
    }

    public Builder mergeFrom(org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation other) {
      if (other == org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.getDefaultInstance()) return this;
      if (!other.property_.isEmpty()) {
        if (property_.isEmpty()) {
          property_ = other.property_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensurePropertyIsMutable();
          property_.addAll(other.property_);
        }
        
      }
      setUnknownFields(
          getUnknownFields().concat(other.unknownFields));
      return this;
    }

    public final boolean isInitialized() {
      for (int i = 0; i < getPropertyCount(); i++) {
        if (!getProperty(i).isInitialized()) {
          
          return false;
        }
      }
      return true;
    }

    public Builder mergeFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property> property_ =
      java.util.Collections.emptyList();
    private void ensurePropertyIsMutable() {
      if (!((bitField0_ & 0x00000001) == 0x00000001)) {
        property_ = new java.util.ArrayList<org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property>(property_);
        bitField0_ |= 0x00000001;
       }
    }

    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property> getPropertyList() {
      return java.util.Collections.unmodifiableList(property_);
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public int getPropertyCount() {
      return property_.size();
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property getProperty(int index) {
      return property_.get(index);
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder setProperty(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensurePropertyIsMutable();
      property_.set(index, value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder setProperty(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.Builder builderForValue) {
      ensurePropertyIsMutable();
      property_.set(index, builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder addProperty(org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensurePropertyIsMutable();
      property_.add(value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder addProperty(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensurePropertyIsMutable();
      property_.add(index, value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder addProperty(
        org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.Builder builderForValue) {
      ensurePropertyIsMutable();
      property_.add(builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder addProperty(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property.Builder builderForValue) {
      ensurePropertyIsMutable();
      property_.add(index, builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder addAllProperty(
        java.lang.Iterable<? extends org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property> values) {
      ensurePropertyIsMutable();
      org.jetbrains.kotlin.protobuf.AbstractMessageLite.Builder.addAll(
          values, property_);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder clearProperty() {
      property_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000001);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation.Property property = 1;</code>
     */
    public Builder removeProperty(int index) {
      ensurePropertyIsMutable();
      property_.remove(index);

      return this;
    }

    // @@protoc_insertion_point(builder_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation)
  }

  static {
    defaultInstance = new IrMultiFieldValueClassRepresentation(true);
    defaultInstance.initFields();
  }

  // @@protoc_insertion_point(class_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrMultiFieldValueClassRepresentation)
}
