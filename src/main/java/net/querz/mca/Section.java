package net.querz.mca;

import static net.querz.mca.LoadFlags.*;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Section implements Comparable<Section> {

	private CompoundTag data;
	private byte[] blocks;
	private byte[] add;
	private byte[] blockData;
	private byte[] blockLight;
	private byte[] skyLight;
	private int height;
	int dataVersion;

	public Section(CompoundTag sectionRoot, int dataVersion) {
		this(sectionRoot, dataVersion, ALL_DATA);
	}

	public Section(CompoundTag sectionRoot, int dataVersion, long loadFlags) {
		data = sectionRoot;
		this.dataVersion = dataVersion;
		height = sectionRoot.getNumber("Y").byteValue();

		ByteArrayTag blocks = sectionRoot.getByteArrayTag("Blocks");
		ByteArrayTag add = sectionRoot.getByteArrayTag("Add");
		ByteArrayTag blockData = sectionRoot.getByteArrayTag("Data");
		ByteArrayTag blockLight = sectionRoot.getByteArrayTag("BlockLight");
		ByteArrayTag skyLight = sectionRoot.getByteArrayTag("SkyLight");

    if ((loadFlags & BLOCK_STATES) != 0) {
      this.blocks = blocks != null ? blocks.getValue() : null;
      this.add = add != null ? add.getValue() : null;
      this.blockData = blockData != null ? blockData.getValue() : null;
    }
		if ((loadFlags & BLOCK_LIGHTS) != 0) {
			this.blockLight = blockLight != null ? blockLight.getValue() : null;
		}
		if ((loadFlags & SKY_LIGHT) != 0) {
			this.skyLight = skyLight != null ? skyLight.getValue() : null;
		}
	}

	Section() {}

	@Override
	public int compareTo(Section o) {
		if (o == null) {
			return -1;
		}
		return Integer.compare(height, o.height);
	}

	private static class PaletteIndex {

		CompoundTag data;
		int index;

		PaletteIndex(CompoundTag data, int index) {
			this.data = data;
			this.index = index;
		}
	}

	/**
	 * Checks whether the data of this Section is empty.
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return data == null;
	}

	/**
	* @return the Y value of this section.
	* */
	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Fetches a block state based on a block location from this section.
	 * The coordinates represent the location of the block inside of this Section.
	 * @param blockX The x-coordinate of the block in this Section
	 * @param blockY The y-coordinate of the block in this Section
	 * @param blockZ The z-coordinate of the block in this Section
	 * @return The block state data of this block.
	 */
	public short getBlockStateAt(int blockX, int blockY, int blockZ) {
		return getBlockStateAt(getBlockIndex(blockX, blockY, blockZ));
	}

	private short getBlockStateAt(int index) {
    if (this.blocks == null) {
      return 0;
    }

    var block = this.blocks[index];
    var add = this.add != null ? this.add[index >> 1] : 0;

    if (add != 0) {
      block |= (index & 1) == 0 ? (add & 0x0F) << 8 : (add & 0xF0) << 4;
    }

    return (short) (block & 0xFF);
	}

	public void setBlockStateAt(int blockX, int blockY, int blockZ, short blockId, byte blockData) {
    var blockIndex = getBlockIndex(blockX, blockY, blockZ);
    var halfBlockIndex = blockIndex >> 1;

    if (this.blocks == null){
      this.blocks = new byte[4096];
    }

    this.blocks[blockIndex] = (byte) (blockId & 0xFF);

    if ((blockId & 0xF00) != 0) {
      if (this.add == null) {
        this.add = new byte[2048];
      }

      var oldBlockIndex = this.add[halfBlockIndex];
      if ((blockIndex & 1) == 0 && (blockId & 0xF00) != 0) {
        this.add[halfBlockIndex] = (byte) ((oldBlockIndex & 0xF0) | ((blockId & 0xF00) >> 8));
      } else if ((blockIndex & 1) == 1 && (blockId & 0xF00) != 0) {
        this.add[halfBlockIndex] = (byte) ((oldBlockIndex & 0x0F) | ((blockId & 0xF00) >> 4));
      }
    }

    if (blockData != 0) {
      if (this.blockData == null) {
        this.blockData = new byte[2048];
      }

      var oldBlockData = this.blockData[halfBlockIndex];
      if ((blockIndex & 1) == 0) {
        this.blockData[halfBlockIndex] = (byte) ((oldBlockData & 0xF0) | (blockData & 0xF));
      } else if ((blockIndex & 1) == 1) {
        this.blockData[halfBlockIndex] = (byte) ((oldBlockData & 0x0F) | ((blockData & 0xF) << 4));
      }
    }
	}

	int getBlockIndex(int blockX, int blockY, int blockZ) {
		return ((blockY & 0xF) << 8) + ((blockZ & 0xF) << 4) + (blockX & 0xF);
	}

	/**
	 * @return The block light array of this Section
	 */
	public byte[] getBlockLight() {
		return blockLight;
	}

	/**
	 * Sets the block light array for this section.
	 * @param blockLight The block light array
	 * @throws IllegalArgumentException When the length of the array is not 2048
	 */
	public void setBlockLight(byte[] blockLight) {
		if (blockLight != null && blockLight.length != 2048) {
			throw new IllegalArgumentException("BlockLight array must have a length of 2048");
		}
		this.blockLight = blockLight;
	}

	/**
	 * @return The sky light values of this Section
	 */
	public byte[] getSkyLight() {
		return skyLight;
	}

	/**
	 * Sets the sky light values of this section.
	 * @param skyLight The custom sky light values
	 * @throws IllegalArgumentException If the length of the array is not 2048
	 */
	public void setSkyLight(byte[] skyLight) {
		if (skyLight != null && skyLight.length != 2048) {
			throw new IllegalArgumentException("SkyLight array must have a length of 2048");
		}
		this.skyLight = skyLight;
	}

	/**
	 * Creates an empty Section with base values.
	 * @return An empty Section
	 */
	public static Section newSection() {
		Section s = new Section();
		s.blocks = new byte[4096];
		s.add = new byte[2048];
		s.blockData = new byte[2048];
		s.data = new CompoundTag();
		return s;
	}

	/**
	 * Updates the raw CompoundTag that this Section is based on.
	 * This must be called before saving a Section to disk if the Section was manually created
	 * to set the Y of this Section.
	 * @param y The Y-value of this Section
	 * @return A reference to the raw CompoundTag this Section is based on
	 */
	public CompoundTag updateHandle(int y) {
		data.putByte("Y", (byte) y);
		if (blocks != null) {
			data.putByteArray("Blocks", blocks);
		}
		if (add != null) {
			data.putByteArray("Add", add);
		}
		if (blockData != null) {
			data.putByteArray("Data", blockData);
		}
		if (blockLight != null) {
			data.putByteArray("BlockLight", blockLight);
		}
		if (skyLight != null) {
			data.putByteArray("SkyLight", skyLight);
		}
		return data;
	}

	public CompoundTag updateHandle() {
		return updateHandle(height);
	}
}
