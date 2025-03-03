import { useState } from 'react'
import './App.css'

interface PurchaseDetails {
  amount: number;
  period: number;
}

interface PurchaseRequest {
  personalId: string;
  details: PurchaseDetails;
}

interface PurchaseResponse {
  approved: boolean;
  details: PurchaseDetails | null;
  message: string;
}

function App() {
  const [personalId, setPersonalId] = useState('');
  const [amount, setAmount] = useState('');
  const [months, setMonths] = useState('6');
  const [response, setResponse] = useState<PurchaseResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [showJson, setShowJson] = useState(false);

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (value === '') {
      setAmount('');
      return;
    }
    
    const number = parseFloat(value);
    if (!isNaN(number)) {
      if (number < 200) {
        setAmount('200');
      } else if (number > 5000) {
        setAmount('5000');
      } else {
        setAmount(number.toFixed(2));
      }
    }
  };

  const handleMonthsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (value === '') {
      setMonths('');
      return;
    }
    
    const number = parseInt(value);
    if (!isNaN(number)) {
      if (number < 6) {
        setMonths('6');
      } else if (number > 24) {
        setMonths('24');
      } else {
        setMonths(number.toString());
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setShowJson(false);
    
    try {
      const request: PurchaseRequest = {
        personalId,
        details: {
          amount: parseFloat(amount),
          period: parseInt(months)
        }
      };

      const res = await fetch('http://13.61.11.131:8080/api/evaluate-purchase', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });
      
      const data = await res.json();
      setResponse(data);
    } catch (error) {
      console.error('Error:', error);
      setResponse({
        approved: false,
        details: null,
        message: 'Error processing request'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <h1>Purchase Approval System</h1>
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="personalId">Personal ID:</label>
          <input
            type="text"
            id="personalId"
            value={personalId}
            onChange={(e) => setPersonalId(e.target.value)}
            required
            placeholder="Enter personal ID"
          />
        </div>

        <div className="form-group">
          <label htmlFor="amount">Amount (€):</label>
          <input
            type="number"
            id="amount"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            onBlur={handleAmountChange}
            required
            min="200"
            max="5000"
            step="0.01"
            placeholder="Enter amount (200-5000)"
          />
        </div>

        <div className="form-group">
          <label htmlFor="months">Payment Period (months):</label>
          <input
            type="number"
            id="months"
            min="6"
            max="24"
            value={months}
            onChange={(e) => setMonths(e.target.value)}
            onBlur={handleMonthsChange}
            required
            placeholder="Enter period (6-24)"
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? 'Processing...' : 'Evaluate Purchase'}
        </button>
      </form>

      {response && (
        <div className={`response ${response.approved ? 'approved' : 'denied'}`}>
          <h2>{response.approved ? 'Approved!' : 'Denied'}</h2>
          <p>{response.message}</p>
          <button 
            onClick={() => setShowJson(!showJson)}
            className="json-button"
          >
            {showJson ? 'Hide JSON' : 'Show JSON'}
          </button>
          {showJson && (
            <pre className="json-response">
              {JSON.stringify(response, null, 2)}
            </pre>
          )}
        </div>
      )}
    </div>
  )
}

export default App
