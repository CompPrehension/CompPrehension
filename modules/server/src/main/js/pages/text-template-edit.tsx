import React, {useEffect, useState} from 'react';
import {Alert, Button, Container, Form, InputGroup, Spinner, Table} from 'react-bootstrap';
import {API_URL} from "../appconfig";

interface TextTemplateEdit {
  templateLocation: string;
  subLocationName: string;
  id: number;
  ownerName: string;
  locCode: string;
  propertyName: string;
  value: string;
}

const API_BASE_URL = `${API_URL}/api/text-template-edit`;

export const TextTemplateEditTable: React.FC = () => {
  const [data, setData] = useState<TextTemplateEdit[]>([]);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSearching, setIsSearching] = useState<boolean>(false);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Функция загрузки данных с возможностью поиска
  const fetchData = async (searchQuery: string = '') => {
    try {
      setIsLoading(true);
      let url = `${API_BASE_URL}/get-all`;

      if (searchQuery) {
        url = `${API_BASE_URL}?value=${encodeURIComponent(searchQuery)}`;
      }

      const response = await fetch(url, {
      method: 'GET',
      headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      setData(result);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch data');
      console.error('Fetch error:', err);
    } finally {
      setIsLoading(false);
      setIsSearching(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Обработчик поиска
  const handleSearch = () => {
    setIsSearching(true);
    fetchData(searchTerm);
  };

  // Обработчик сохранения
  const handleSave = async () => {
    try {
      setIsSaving(true);
      const response = await fetch(`${API_BASE_URL}/save`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

//       alert('Data saved successfully!');
      fetchData(searchTerm);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save data');
      console.error('Save error:', err);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) return (
    <Container className="mt-5 text-center">
      <Spinner animation="border" variant="primary" />
      <p className="mt-2">Loading...</p>
    </Container>
  );

  if (error) return (
    <Container className="mt-5">
      <Alert variant="danger">Error: {error}</Alert>
      <Button variant="primary" onClick={() => fetchData(searchTerm)}>Retry</Button>
    </Container>
  );

  return (
    <Container className="mt-4">
      <h1 className="mb-4">Text Templates</h1>

      <div className="mb-4">
        <InputGroup className="mb-2">
          <Form.Control
            type="search"
            placeholder="Search..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e: React.KeyboardEvent<HTMLInputElement>) => e.key === 'Enter' && handleSearch()}
          />
          <Button
            variant="primary"
            onClick={handleSearch}
            disabled={isSearching}
          >
            {isSearching ? (
              <>
                <Spinner as="span" animation="border" size="sm" /> Searching...
              </>
            ) : 'Search'}
          </Button>
        </InputGroup>

        <div className="text-end">
          <Button
            variant="success"
            onClick={handleSave}
            disabled={isSaving}
          >
            {isSaving ? (
              <>
                <Spinner as="span" animation="border" size="sm" /> Saving...
              </>
            ) : 'Save'}
          </Button>
        </div>
      </div>

      <Table striped bordered hover responsive>
        <thead className="table-light">
          <tr>
            <th style={{width: '30%'}}>Key</th>
            <th style={{width: '70%'}}>Value</th>
          </tr>
        </thead>
        <tbody>
        {data.map((item, index) => (
            <tr key={item.id}>
              <td className="align-middle">
                {item.templateLocation}/{item.subLocationName}/{item.ownerName} - {item.locCode}.{item.propertyName}
              </td>
              <td>
                <Form.Control
                    as="textarea"
                    rows={Math.ceil(item.value.length / 60)}
                  type="text"
                  value={item.value}
                  onChange={(e) => {
                    setData(prevData =>
                        prevData.toSpliced(index, 1, {...item, value: e.target.value})
                    );
                  }}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
};