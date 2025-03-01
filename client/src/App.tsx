import { useState } from 'react'
import './App.css'

interface PurchaseResponse {
  approved: boolean;
  approvedAmount: number | null;
  message: string;
}

function App() {
  const [personalId, setPersonalId] = useState('');
  const [amount, setAmount] = useState('');
  const [months, setMonths] = useState('12');
  const [response, setResponse] = useState<PurchaseResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const res = await fetch('http://localhost:8080/api/evaluate-purchase', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          personalId,
          requestedAmount: parseFloat(amount),
          months: parseInt(months),
        }),
      });
      
      const data = await res.json();
      setResponse(data);
    } catch (error) {
      console.error('Error:', error);
      setResponse({
        approved: false,
        approvedAmount: null,
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
            required
            min="200"
            max="5000"
            step="0.01"
            placeholder="Enter amount (200-5000)"
          />
        </div>

        <div className="form-group">
          <label htmlFor="months">Payment Period (months):</label>
          <select
            id="months"
            value={months}
            onChange={(e) => setMonths(e.target.value)}
            required
          >
            {Array.from({length: 19}, (_, i) => i + 6).map(num => (
              <option key={num} value={num}>{num} months</option>
            ))}
          </select>
        </div>

        <button type="submit" disabled={loading}>
          {loading ? 'Processing...' : 'Evaluate Purchase'}
        </button>
      </form>

      {response && (
        <div className={`response ${response.approved ? 'approved' : 'denied'}`}>
          <h2>{response.approved ? 'Approved!' : 'Denied'}</h2>
          {response.approvedAmount && (
            <p>Approved amount: €{response.approvedAmount.toFixed(2)}</p>
          )}
          <p>{response.message}</p>
        </div>
      )}
    </div>
  )
}

export default App
