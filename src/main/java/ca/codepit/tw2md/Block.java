package ca.codepit.tw2md;

import java.util.ArrayList;
import java.util.List;

import static ca.codepit.tw2md.Main.*;

/**
 * @author evan
 */
public class Block {

	private final BLOCK_TYPE blockType;

	private final List<String> lines;

	public Block(BLOCK_TYPE blockType, List<String> lines) {

		this.blockType = blockType;
		this.lines = lines;
	}

	public Block(BLOCK_TYPE blockType, String line) {

		this.blockType = blockType;

		List<String> l = new ArrayList<>();
		l.add(line);
		this.lines = l;
	}

	public BLOCK_TYPE getBlockType() {

		return blockType;
	}

	public List<String> getLines() {

		return lines;
	}

	@Override
	public String toString() {

		return "Block{" +
						"blockType=" + blockType +
						", lines=" + lines +
						'}';
	}
}
