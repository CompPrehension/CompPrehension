import React, {useEffect, useState} from "react";
import {Bug} from "lucide-react";
import {Optional} from "./optional";

interface DebugButtonProps {
  metadataId: number;
  attemptId?: number;
}

const DebugButton: React.FC<DebugButtonProps> = ({ metadataId, attemptId }) => {
  const [clicked, setClicked] = useState(false);

  useEffect(() => {
    setClicked(false);
  }, [metadataId, attemptId]);

  return (
    <div
        className="position-absolute"
        style={{ bottom: '0.5rem', right: '0.5rem', zIndex: 1050 }}
      >
        {!clicked ? (
          <button
            type="button"
            className="btn btn-light border-0"
            style={{
              backgroundColor: "transparent",
              color: "transparent",
              padding: "0.2rem",
              transition: "all 0.3s ease",
            }}
            onMouseEnter={(e) => {
              (e.currentTarget.style.color = "#dc3545"); // Красный цвет при наведении
              (e.currentTarget.style.backgroundColor = "#f8d7da"); // Светло-красный фон
            }}
            onMouseLeave={(e) => {
              (e.currentTarget.style.color = "transparent");
              (e.currentTarget.style.backgroundColor = "transparent");
            }}
            onClick={() => setClicked(true)}
          >
            <Bug size={18} />
          </button>
        ) : (
          <div className="alert alert-danger p-2 mb-0" role="alert">
            <div><small><strong>Metadata ID:</strong> {metadataId}</small></div>
            <Optional isVisible={attemptId !== undefined}>
              <div><small><strong>Attempt ID:</strong> {attemptId}</small></div>
            </Optional>
          </div>
        )}
    </div>
  );
};

export default DebugButton;
