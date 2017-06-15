export class BalanceInfo {
    balance: number;
	exposure: number;
	timestamp: number;

    constructor ( balance: number, exposure: number, timestamp: number) {
        this.balance = balance;
        this.exposure = exposure;
        this.timestamp = timestamp;
    }
}