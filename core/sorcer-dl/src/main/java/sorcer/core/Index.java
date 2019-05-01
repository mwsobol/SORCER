package sorcer.core;

import sorcer.service.Arg;
import java.io.Serializable;

public class Index implements Serializable, Comparable {
	public enum Direction { column, row };

	private int index;

	private Direction type = Direction.column;

	public Index(int index) {
		this.index = index;
	}

    public Index(int index, Direction type) {
        this.index = index;
        this.type = type;
    }

    @Override
	public boolean equals(Object object) {
		if (object instanceof Index)
			return this.index == (((Index) object).getIndex());
		else
			return false;
	}

	@Override
	public int hashCode() {
		return new Integer(index).hashCode();
	}

	@Override
	public String toString() {
		return "" + index;
	}

	@Override
	public int compareTo(Object o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Index)
			return new Integer(index).compareTo(((Index) o).getIndex());
		else
			return -1;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Direction getType() {
		return type;
	}

	public void setType(Direction type) {
		this.type = type;
	}

//	public String getName() {
//		return ""+index;
//	}
}
