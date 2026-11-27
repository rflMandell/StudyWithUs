import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock, Search } from 'lucide-react';
import { userService } from '../services/userService';
import { UserSearchItem } from '../types/auth';
import { useAuth } from '../hooks/useAuth';

export const UserSearchBar: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserSearchItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  useEffect(() => {
    const trimmed = query.trim();
    if (!trimmed) {
      setResults([]);
      setIsLoading(false);
      setIsOpen(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setIsOpen(true);

    const timer = setTimeout(async () => {
      try {
        const data = await userService.searchUsers(trimmed);
        if (!cancelled) {
          setResults(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          setResults([]);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }, 200);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query]);

  const handleSelectUser = (item: UserSearchItem) => {
    setIsOpen(false);
    setQuery('');
    setResults([]);
    const targetPath = item.id === user?.id ? '/profile' : `/profile/${item.id}`;
    navigate(targetPath);
  };

  return (
    <div ref={containerRef} style={{ position: 'relative', minWidth: '220px', maxWidth: '300px', flex: '1 1 220px' }}>
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        <Search
          size={15}
          color="var(--text-muted)"
          style={{ position: 'absolute', left: '0.65rem', pointerEvents: 'none' }}
        />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => {
            if (query.trim()) {
              setIsOpen(true);
            }
          }}
          placeholder="Buscar perfis por nome ou @..."
          aria-label="Buscar perfis"
          className="input"
          style={{
            width: '100%',
            padding: '0.4rem 0.75rem 0.4rem 2.1rem',
            fontSize: '0.85rem',
          }}
        />
      </div>

      {isOpen && query.trim() && (
        <div
          role="listbox"
          aria-label="Resultados da busca de perfis"
          style={{
            position: 'absolute',
            top: 'calc(100% + 0.35rem)',
            left: 0,
            right: 0,
            backgroundColor: 'var(--bg-card)',
            border: '1px solid var(--border)',
            borderRadius: '8px',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.12)',
            zIndex: 50,
            maxHeight: '320px',
            overflowY: 'auto',
          }}
        >
          {isLoading ? (
            <div style={{ padding: '0.75rem 1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Buscando perfis...
            </div>
          ) : results.length === 0 ? (
            <div style={{ padding: '0.75rem 1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Nenhum perfil encontrado.
            </div>
          ) : (
            results.map((item) => (
              <button
                key={item.id}
                type="button"
                role="option"
                onClick={() => handleSelectUser(item)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.65rem',
                  padding: '0.6rem 0.85rem',
                  border: 'none',
                  borderBottom: '1px solid var(--border)',
                  background: 'transparent',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--primary-light)',
                    color: 'var(--primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                    fontSize: '0.85rem',
                    overflow: 'hidden',
                    flexShrink: 0,
                  }}
                >
                  {item.avatarUrl ? (
                    <img
                      src={item.avatarUrl}
                      alt={item.name}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    item.name.charAt(0).toUpperCase()
                  )}
                </div>

                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <span
                      style={{
                        fontWeight: 600,
                        fontSize: '0.875rem',
                        color: 'var(--text-main)',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {item.name}
                    </span>
                    {item.isPrivate && <Lock size={12} color="var(--text-muted)" aria-label="Perfil privado" />}
                  </div>
                  <div style={{ fontSize: '0.775rem', color: 'var(--text-muted)' }}>@{item.username}</div>
                </div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
};
