export class StockInfo {
    code: string;
    startingPrice: number;
	price: number;
	change: number;
	holding: number;

    constructor(code: string, startingPrice: number, price: number, change: number, holding: number) {
        this.code = code;
        this.startingPrice = startingPrice;
	    this.price = price;
	    this.change = change;
	    this.holding = holding;
    }

}