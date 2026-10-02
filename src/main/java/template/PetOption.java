package template;

public class PetOption {
	public int value;
	public int maxValue;
	public byte id;

	public PetOption(int id, int value, int maxValue) {
		this.value = value;
		this.maxValue = maxValue;
		this.id = (byte) id;
	}
}
