
package template;


public class box_item_template {
    public short id;
    public short quantity;
    public byte catagory;
    public box_item_template(){}
    public box_item_template(short id, short quant, byte cat){
        this.id= id;
        this.quantity =quant;
        this.catagory = cat;
    }
}
