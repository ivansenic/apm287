import { Component, ViewChild } from '@angular/core';
import { NavController } from 'ionic-angular';
import { BalanceInfo } from '../../common/BalanceInfo';
import { StockInfo } from '../../common/StockInfo';
import { Http, Response } from "@angular/http";
import { LoadingController, AlertController, ToastController } from 'ionic-angular';
import { Chart } from 'chart.js';
import 'rxjs/Rx';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {
  @ViewChild('lineCanvas') lineCanvas;
  stocks: StockInfo[] = [];
  balance: BalanceInfo = new BalanceInfo(0, 0, 0);
  chart: Chart;
  serviceIp: string = "localhost";
  servicePort: string = "8080";
  showGraph: boolean = false;

  constructor(private http: Http, private loadingCtrl: LoadingController, private alertCtrl: AlertController, private toastCtrl: ToastController) {
   
  }

  buy(code: string) {
    let confirm = this.alertCtrl.create();
    confirm.setTitle("Confirm Sell");
    confirm.setMessage("Amount of " + code + " stocks to buy?");
    confirm.addInput({
      type: 'number',
      label: 'Amount',
      value: '1'
    });
    confirm.addButton('Cancel');
    confirm.addButton({
      text: 'OK',
      handler: (data) => {
        console.log(data);
        this.http.get('http://' + this.serviceIp + ':' + this.servicePort + '/buy?c=' + code + '&s=' + data['0'])
          .map((response: Response) => {
            return response.json();
          })
          .subscribe(
            (dat) => {
              if (!dat.success) {
                let toast = this.toastCtrl.create({
                  message: 'Error buying stocks. ' + dat.reason,
                  showCloseButton: true,
                  position: 'bottom'
                });
                toast.present();
              } else {
                let stockInfo = dat.stockInfo;
                for (var i = 0; i < this.stocks.length; i++) {
                  if (this.stocks[i].code === stockInfo.code) {
                    this.stocks[i] = stockInfo;
                    break;
                  }
                }
              }
            },
            (err) => {
              let toast = this.toastCtrl.create({
                  message: 'Error buying stocks. ' + err,
                  showCloseButton: true,
                  position: 'bottom'
              });
              toast.present();
            }
          );
        }
    });
    confirm.present();
  }

  sell(code: string) {
    let confirm = this.alertCtrl.create();
    confirm.setTitle("Confirm Sell");
    confirm.setMessage("Amount of " + code + " stocks to sell?");
    confirm.addInput({
      type: 'number',
      label: 'Amount',
      value: '1'
    });
    confirm.addButton('Cancel');
    confirm.addButton({
      text: 'OK',
      handler: (data) => {
        console.log(data);
        this.http.get('http://' + this.serviceIp + ':' + this.servicePort + '/sell?c=' + code + '&s=' + data['0'])
          .map((response: Response) => {
            return response.json();
          })
          .subscribe(
            (dat) => {
              if (!dat.success) {
                let toast = this.toastCtrl.create({
                  message: 'Error selling stocks. ' + dat.reason,
                  showCloseButton: true,
                  position: 'bottom'
                });
                toast.present();
              } else {
                let stockInfo = dat.stockInfo;
                for (var i = 0; i < this.stocks.length; i++) {
                  if (this.stocks[i].code === stockInfo.code) {
                    this.stocks[i] = stockInfo;
                    break;
                  }
                }
              }
            },
            (err) => {
              let toast = this.toastCtrl.create({
                  message: 'Error selling stocks. ' + err,
                  showCloseButton: true,
                  position: 'bottom'
              });
              toast.present();
            }
          );
        }
    });
    confirm.present();
  }

  fetchStocks() {
    console.log("Fetching stocks..");
    this.http.get('http://' + this.serviceIp + ':' + this.servicePort + '/stocks')
      .map((response: Response) => {
        return response.json();
      })
      .subscribe(
        (data) => {
          this.stocks = data.stockPrices;
        },
        (err) => {
          console.log(err);
        }
      );
  }

  fetchBalace() {
    console.log("Fetching balance..");
    this.http.get('http://' + this.serviceIp + ':' + this.servicePort + '/balance')
      .map((response: Response) => {
        return response.json();
      })
      .subscribe(
        (data) => {
          var balanceUpdate = data.balanceInfo;
          this.balance = balanceUpdate;

          // do nothing when we don't need to
          if (!this.showGraph) {
            return;
          }

          // create chart if needed
          if (!this.chart) {
            console.log("Creating chart..");
            this.createChart();
          }
          
          this.chart.data.labels.push(new Date(balanceUpdate.timestamp).toDateString());
          this.chart.data.datasets.forEach((dataset) => {
            console.log("Dataset " + dataset.label);
            if ('Balance' === dataset.label) {
               dataset.data.push(balanceUpdate.balance);
            } else if ('Exposure' === dataset.label) {
              dataset.data.push(balanceUpdate.exposure);
            }
          });
          this.chart.update();
        },
        (err) => {
          console.log(err);
        }
      );
  }

  ionViewDidLoad() {
    var loading = this.loadingCtrl.create({
        content: "Fetching data..",
        duration: 1000
    });
    this.fetchStocks();
    this.fetchBalace();

    var thisVar = this;
    setInterval(function() { 
      thisVar.fetchStocks();
      thisVar.fetchBalace();
    }, 5000);
  }

  createChart() {
     this.chart = new Chart(this.lineCanvas.nativeElement, {
       type: 'line',
       responsive: true,
       options: {
         scales: {
           yAxes: [{
                stacked: true
            }]
         }
       },
       data: {
         labels: [],
         datasets: [
          {
            label: "Balance",
            fill: true,
            lineTension: 0.1,
            backgroundColor: "rgba(75,192,192,0.4)",
          },
          {
            label: "Exposure",
            fill: true,
            lineTension: 0.1,
            backgroundColor: "rgba(120,155,0,0.4)",
          }
        ]
      }
     });
  }

}
